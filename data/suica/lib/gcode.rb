require 'graticule'

@geocoder = Graticule.service(:google).new ""

def geocode(text)
  redis_key = [ 'gcode', text ].join('_')
  unless @redis.exists(redis_key)
    begin
      loc = @geocoder.locate(text)
      @redis.set(redis_key, [ loc.latitude, loc.longitude ].to_json)
    rescue Exception => ex
      raise ex unless ex.message == "unknown address"
      @redis.set(redis_key, [ nil, nil ].to_json)
    end
  end
  JSON::parse(@redis.get(redis_key))
end

