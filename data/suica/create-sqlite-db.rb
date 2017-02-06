#!/usr/bin/ruby
# encoding: utf-8

require 'rubygems'
require 'csv'
require 'sqlite3'
require 'redis'
require "unicode_utils/nfkc"
require './lib/gtrans'
require './lib/gcode'

COLS = %w(AreaCode LineCode StationCode CompanyName CompanyName_en LineName LineName_en StationName StationName_en StationNote Latitude Longitude)
BUS_COLS = %w(LineCode StationCode CompanyName LineName StationName Note CompanyName_en LineName_en StationName_en)

DB_OUT  = '../out/StationCode.db'
CSV_OUT = '../out/StationCode.csv'

RAIL_TABLE = 'StationCode'
BUS_TABLE  = 'IruCaStationCode'

VERSION = 3

@redis = Redis.new

@english = {
  # Lines
  '大阪環状'           => 'Ōsaka Loop',
  '白新線'             => 'Hakushin',
  '鴨東'               => 'Ōtō',
  '東西'               => 'Tōzai',
  '総武本'             => 'Sōbu',
  '中央本'             => 'Chūō',
  '東京モノレール羽田' => 'Tokyo Monorail',
  '東京臨海新交通臨海' => 'Tokyo Waterfront New Transit'
}

# Companies
CSV.foreach('data/RailCompanies.csv', headers: :first_row) do |row|
  ja_name = row['Japanese']
  en_name = row['English (Fixed)']
  @english[ja_name] = en_name
end

def translate(ja_text, translate = true, transliterate = false)
  return @english[ja_text] if @english.include?(ja_text) 
  
  translated, translit = google_translate(ja_text)

  # Clean up transliteration
  translit = translit.gsub(' ', '').gsub("'", '')

  if translated.contains_cjk? or translated == ja_text or translit == translated
    # Bad translation
    translit
  elsif translit.contains_cjk?
    # Bad transliteration
    translated
  elsif ja_text.contains_kana?
    # Always return translation if there's kana to avoid engrlish
    translated
  elsif transliterate and translate
    "#{translit} (#{translated})"
  elsif transliterate
    translit
  else
    translated
  end
end

def translate_company(ja_text)
  en = translate(ja_text)
  bad_suffixes = [ 'Corp.', 'Co., Ltd.', 'Corporation' ]
  bad_suffixes.inject(en) { |en, bad| en.gsub(/ #{Regexp.escape(bad)}$/, '') }
end

def translate_line(ja_text)
  line_num = UnicodeUtils.nfkc(ja_text).match(/(\d+)号/) {|m| m[1] }
  if line_num
    ja_text = ja_text.sub(/\d+号/, '')
    ja_text = ja_text.sub('線', '')
    return "##{line_num}" if ja_text.blank?
  end
  en = translate(ja_text, false, true)
  line_num ? "##{line_num} #{en}" : en
end

def geocode_station(station_name)
  geocode([ station_name, '駅' ].join(' '))
end

FileUtils.rm(CSV_OUT) if File.exists?(CSV_OUT)
FileUtils.rm(DB_OUT) if File.exists?(DB_OUT)

@db = SQLite3::Database.new(DB_OUT)
@db.execute("PRAGMA user_version = #{VERSION}")
@db.execute("CREATE TABLE #{RAIL_TABLE} (_id INTEGER PRIMARY KEY AUTOINCREMENT, #{COLS.join(',')})")
@db.execute("CREATE INDEX line_sta_index ON #{RAIL_TABLE} (LineCode, StationCode)")
@db.execute("CREATE INDEX area_line_sta_index ON #{RAIL_TABLE} (AreaCode, LineCode, StationCode)")
@db.execute("CREATE TABLE 'android_metadata' ('locale' TEXT DEFAULT 'en_US')")
@db.execute("INSERT INTO 'android_metadata' VALUES ('en_US')")

CSV.open(CSV_OUT, "wb") do |csv|
  csv << COLS
  CSV.foreach('data/SFCardFan-RailStationCode.csv', headers: :first_row) do |row|
    area_code    = row['AreaCode']
    line_code    = row['LineCode']
    station_code = row['StationCode']
    company_name = row['CompanyName']
    line_name    = row['LineName']
    station_name = row['StationName']
    station_note = row['Note']

    company_name_en = translate_company(company_name)
    line_name_en    = translate_line(line_name)
    station_name_en = translate(station_name, false, true)

    latitude, longitude = geocode_station(station_name)

    new_row = [
      area_code, 
      line_code, 
      station_code,
      company_name, 
      company_name_en,
      line_name, 
      line_name_en,
      station_name, 
      station_name_en,
      station_note,
      latitude,
      longitude
    ]

    puts new_row.join(',')
    csv << new_row

    @db.execute("INSERT INTO #{RAIL_TABLE} (#{COLS.join(',')}) VALUES(#{COLS.length.times.map{'?'}.join(',')})", *new_row)
  end
end

# NOTE: This data is from the nfc-felica project... the english has not been 
# vetted at *all* and is probably a huge disaster.
@db.execute("CREATE TABLE #{BUS_TABLE} (_id INTEGER PRIMARY KEY AUTOINCREMENT, #{BUS_COLS.join(',')})")
@db.execute("CREATE INDEX iruca_line_sta_index ON #{BUS_TABLE} (LineCode, StationCode)")
CSV.foreach('data/nfcfelica-iruca.csv', headers: :first_row) do |row|
  line_code       = row['LineCode']
  station_code    = row['StationCode']
  company_name    = row['CompanyName']
  line_name       = row['LineName']
  station_name    = row['StationName']
  note            = row['Note']
  company_name_en = row['CompanyName_en']
  line_name_en    = row['LineName_en']
  station_name_en = row['StationName_en']

  new_row = [
    line_code,
    station_code,
    company_name,
    line_name,
    station_name,
    note,
    company_name_en,
    line_name_en,
    station_name_en
  ]

  puts new_row.join(',')

  @db.execute("INSERT INTO #{BUS_TABLE} (#{BUS_COLS.join(',')}) VALUES(#{BUS_COLS.length.times.map{'?'}.join(',')})", *new_row)
end

puts "Wrote #{DB_OUT} and #{CSV_OUT}"
