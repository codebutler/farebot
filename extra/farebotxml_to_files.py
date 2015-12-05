#!/usr/bin/env python
"""
farebotxml_to_files.py - converts Farebot XML files to binary files for easier
analysis

Copyright 2015 Michael Farrell <micolous+git@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

"""

from argparse import ArgumentParser, FileType
from json import dumps
from lxml import objectify
from os.path import join
from zipfile import ZipFile


def zipify(input_xml, output_zipf):
	output_zip = ZipFile(output_zipf, 'w')
	xml = objectify.parse(input_xml)
	root = xml.getroot()
	assert root.tag == 'cards'

	# iterate through cards
	for card in root.iterchildren():
		assert card.tag == 'card'
		scanned_at = card.get('scanned_at')
		card_id = card.get('id')
		card_type = card.get('type')
		
		card_dir = 'scan_%s_%s' % (scanned_at, card_id)

		sectors = card.find('sectors')
		if sectors is not None:
			# Mifare classic card
			# Iterate sectors
			for sector in sectors.findall('sector'):
				sector_id = sector.get('index')
				if sector.get('unauthorized') == 'true':
					# Locked sector, skip and leave marker
					output_zip.writestr(join(card_dir, sector_id, '.unauthorized'), '')
					continue

				for block in sector.find('blocks').findall('block'):
					# Lets pull some blocks!
					assert block.get('type') == 'data'
					output_zip.writestr(join(card_dir, sector_id, block.get('index')), str(block.find('data')).decode('base64'))

		applications = card.find('applications')
		if applications is not None:
			# Mifare DESfire or like
			for application in applications.findall('application'):
				application_id = application.get('id')
				for f in application.find('files').findall('file'):
					file_id = f.get('id')
					error = f.find('error')
					if error is not None:
						continue
					
					output_zip.writestr(join(card_dir, application_id, file_id), str(f.find('data')).decode('base64'))

	output_zip.close()
	output_zipf.close()

def main():
	parser = ArgumentParser()
	parser.add_argument('input_xml', nargs=1, type=FileType('rb'),
		help='Farebot XML file to read')

	parser.add_argument('-o', '--output', type=FileType('wb'),
		help='Output ZIP file to write')

	options = parser.parse_args()
	zipify(options.input_xml[0], options.output)


if __name__ == '__main__':
	main()

