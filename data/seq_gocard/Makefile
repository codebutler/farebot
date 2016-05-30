.PHONY: clean

all: seq_go_stations.db3

seq_go_stations.db3: mapping.csv SEQ.zip ../../extra/compile_stops_from_gtfs.py
	python ../../extra/compile_stops_from_gtfs.py SEQ.zip -m mapping.csv -e 'zone_id' --extra-fields-from-child -o $@

SEQ.zip:
	wget -N http://translink.com.au/sites/default/files/assets/resources/about-translink/reporting-and-publications/open-data/gtfs/$@

clean:
	rm -f SEQ.zip seq_gocard_stations.db3

