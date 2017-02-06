# encoding: utf-8

require 'nestful'

USER_AGENT = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.8 (KHTML, like Gecko) Chrome/17.0.928.0 Safari/535.8' 
REFERRER   = 'http://translate.google.com/'
URL        = 'http://translate.google.com/translate_a/t'

def google_translate(ja_text)
  return nil if ja_text.nil? or ja_text.empty?

  redis_key = [ 'gtrans', ja_text ].join('_')

  unless @redis.exists(redis_key)
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
    translit   = result[3]

    @redis.set redis_key, [ translated, translit ].to_json
  end
  
  JSON::parse(@redis.get(redis_key))
end

class String
  def contains_cjk?
    !!(self =~ /\p{Han}|\p{Katakana}|\p{Hiragana}\p{Hangul}/)
  end
  def contains_kana?
    !!(self =~ /\p{Katakana}/)
  end
end
