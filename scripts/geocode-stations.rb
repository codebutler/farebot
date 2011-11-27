# encoding: utf-8

require './common.rb'
require 'graticule'

@geocoder = Graticule.service(:google).new ""

def geocode_station(station_name)
  @geocoder.locate [ station_name, '駅' ].join(' ')
end

db = SQLite3::Database.new( "../assets/StationCode.db" )
db.results_as_hash = true

TABLES.each do |table_name|
  puts table_name

  i = 0

  count = db.get_first_value("SELECT COUNT(*) FROM #{table_name} WHERE Latitude IS NULL AND Longitude IS NULL")
  db.execute("SELECT #{COLUMNS.join(',')} FROM #{table_name} WHERE Latitude IS NULL AND Longitude IS NULL") do |row|
    station_name = row["StationName"]

    status = "[#{i}/#{count}]"

    puts "#{status} #{station_name}"

    # Test
    #break if station_name.include?('端末試験用')

    begin
      loc = geocode_station station_name
      puts "#{status} #{loc.latitude} #{loc.longitude}"

      update_sql = "UPDATE #{table_name} 
        SET
          Latitude  = :latitude,
          Longitude = :longitude
        WHERE _id = :id"
      db.prepare update_sql do |stmt|
        r = stmt.execute id: row['_id'], latitude: loc.latitude, longitude: loc.longitude
        raise "Wrong number of rows changed: #{db.changes}" if db.changes != 1
      end

    rescue Exception => ex
      if ex.message == "unknown address"
        puts "#{status} #{ex}"
      else
        raise ex
      end
    end

    sleep 0.1
    i += 1
  end
end
