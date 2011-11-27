#!/usr/bin/ruby
# encoding: utf-8

require 'rubygems'
require 'nestful'
require './common'

class String
  def contains_cjk?
    !!(self =~ /\p{Han}|\p{Katakana}|\p{Hiragana}\p{Hangul}/)
  end
end

USER_AGENT = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.8 (KHTML, like Gecko) Chrome/17.0.928.0 Safari/535.8' 
REFERRER   = 'http://translate.google.com/'
URL        = 'http://translate.google.com/translate_a/t'

JP_TO_EN = {
  # Companies
  '東日本旅客鉄道' => 'JR East',
  'JR東日本'       => 'JR East',
  '西日本旅客鉄道' => 'JR West',
  '北海道旅客鉄道' => 'JR Hokkaido',
  '東海旅客鉄道'   => 'JR Central',
  '九州旅客鉄道'   => 'JR Kyusyu',
  '近畿日本鉄道'   => 'Kintetsu',
  '東葉高速鉄道'   => 'Tōyō Rapid Railway',
  
  # Lines
  '大阪環状' => 'Ōsaka Loop',
  '白新線'   => 'Hakushin',
  '鴨東'     => 'Ōtō',
  '東西'     => 'Tōzai',
  '総武本'   => 'Sōbu',
  '中央本'   => 'Chūō'
}

$translate_cache = {}

def translate(ja_text, prefer_translit = false)
  return nil if ja_text.nil? or ja_text.empty?

  if JP_TO_EN.include?(ja_text)
    return JP_TO_EN[ja_text]
  end

  if $translate_cache.include?(ja_text)
    return $translate_cache[ja_text]
  end

  params = {
    client: 't',
    hl: 'en',
    tl: 'en',
    sl: 'ja',
    text: ja_text
  }
  json = Nestful.get URL, params: params, headers: { 'User-Agent' => USER_AGENT }
  json = json.gsub(',,,', ',')
  json = json.gsub(',,', ',')

  result = JSON::parse(json).flatten
  translated = result[0]
  original   = result[1]
  translit   = result[3].gsub(' ', '')

  english = (prefer_translit or translated == original or translated.contains_cjk?) ? translit : translated
  $translate_cache[ja_text] = english
  english
end

def translate_row(row)
  unless row['CompanyName_en']
    company_name = translate(row['CompanyName']) 
    company_name = company_name.sub(/ Co\., Ltd\.$/, '')
    row['CompanyName_en'] = company_name
  end

  row['LineName_en'] = translate(row['LineName']) unless row['LineName_en']

  unless row['StationName_en']
    station_name = row['StationName'] + '駅'
    station_name = station_name.gsub(/一/, '1')
    station_name = translate(station_name)
    station_name = station_name.sub(/ Station$/i, '')
    station_name = station_name.sub(/-eki$/i, '')
    station_name = station_name.sub(/ -chome/i, '-chome')
    row['StationName_en'] = station_name
  end

  puts "----"
  puts "Company Name: %s %s" % [ row['CompanyName'], company_name       ]
  puts "Line Name: %s %s"    % [ row['LineName'],    row['LineName_en'] ]
  puts "Station Name: %s %s" % [ row['StationName'], station_name       ]

  row
end

def update_row(db, table, row)
  update_sql = "UPDATE #{table} 
    SET
      CompanyName_en = :company_en,
      LineName_en    = :line_en,
      StationName_en = :station_en
    WHERE _id = :id"
  db.prepare update_sql do |stmt|
    r = stmt.execute id: row['_id'], company_en: row['CompanyName_en'], line_en: row['LineName_en'], station_en: row['StationName_en']
    raise "Wrong number of rows changed: #{db.changes}" if db.changes != 1
  end
end

db = SQLite3::Database.new( "../assets/StationCode.db" )
db.results_as_hash = true

TABLES.each do |table_name|
  i = 0
  db.execute("SELECT #{COLUMNS.join(',')} FROM #{table_name}") do |row|
    row = translate_row(row)
    update_row(db, table_name, row)
    i += 1
    puts "#{table_name} #{i}"
  end
end
