#!/usr/bin/env python3
"""
textkeys_to_farebotkeys.py
Takes a newline-seperated list of MIFARE classic keys and converts them to
Farebot's binary format.

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
import base64, struct

def encode_keys(input_file, output_file):
	# first read all the keys and sort them out
	keys = set()
	for line_no, line in enumerate(input_file.readlines()):
		line = line.upper().strip()

		if line.startswith(b';') or line.startswith(b'#') or line == b'':
			# comment, skip this
			continue
		
		if line.startswith(b'0X'):
			# Trim 0x-style values
			line = line[2:]
		
		# check that the key is the right length
		if len(line) != 12:
			print ('Invalid key %r given on line %d (must be 12 characters)' % (line, line_no+1))
			return

		# push to keyset
		keys.add(base64.b16decode(line))

	# We have all the keys, dump it to the file.
	for key in keys:
		output_file.write(key)

	input_file.close()
	output_file.close()
	print ('%d keys added' % (len(keys)))

def main():
	parser = ArgumentParser()
	parser.add_argument(
		'input_file', type=FileType('rb'), nargs=1)
	parser.add_argument(
		'-o', '--output', type=FileType('wb'), required=True)
	
	options = parser.parse_args()
	encode_keys(options.input_file[0], options.output)

if __name__ == '__main__':
	main()

