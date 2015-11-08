#!/usr/bin/env python3
"""
find_in_bitfield.py
Attempts to locate an integer inside of a given file with bitfields

Copyright 2015 Michael Farrell <micolous+git@gmail.com>

Note: This loads the entire file into memory blindly.  Do not use on large file
sizes, as the memory usage of this file is extremely inefficient.

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
import re
import sys

def ord2(x):
	if sys.version_info[0] == 2:
		# On Python2, use "ord"
		return ord(x)
	elif sys.version_info[0] == 3:
		# On Python3, passthrough
		return x
	

def bitfinder(input_data, integer, pad=0, byte_aligned_only=False):
	"""
	returns tuple of bit-indexes (0 indexed) of (big endian, little endian) bit
	offsets where the number appears.
	
	Note that little endian values are reversed and make it so that the bit
	offsets are in "opposite" order (ie: reading as reversed string).
	"""
	# Convert to binary, skipping the first two bytes (0b)
	integer_binary = bin(integer)[2:]
	
	if pad > 0:
		integer_binary = integer_binary.rjust(pad, '0')
	
	# Convert input data to binary
	input_data = [ord2(x) for x in input_data]
	input_binary_be = ''.join(bin(x)[2:].rjust(8, '0') for x in input_data)
	input_binary_le = ''.join(bin(x)[2:].rjust(8, '0') for x in input_data[::-1])
	input_binary_be_revbits = input_binary_le[::-1]
	input_binary_le_revbits = input_binary_be[::-1]
	#print input_binary_be, input_binary_le, integer_binary

	# now start searching.
	results = [[m.start() for m in re.finditer(r'(?=%s)' % integer_binary, y)] for y in (input_binary_be, input_binary_le, input_binary_be_revbits, input_binary_le_revbits)]
	
	if byte_aligned_only:
		results = [filter(lambda x: x%8==0, y) for y in results]
	return results

def main():
	parser = ArgumentParser()
	parser.add_argument('input_file', nargs='+', type=FileType('rb'),
		help='Input file to search. Only use small files, this program is not memory efficient')
	parser.add_argument('-i', '--integer', type=int, required=True,
		help='Integer to search for')
	parser.add_argument('-p', '--pad', type=int, default=0,
		help='Zero-pad the input integer such that the total number of bits is at least this')
	parser.add_argument('-b', '--byte-aligned', action='store_true',
		help='Only return values that are aligned to bytes (divisible by 8)')
	
	options = parser.parse_args()
	for fh in options.input_file:
		results_be, results_le, results_be_revbits, results_le_revbits = \
			bitfinder(fh.read(), options.integer, options.pad,
				options.byte_aligned)

		print ('Filename: %s' % fh.name)
		print ('Integer: %d padded to %d bits' % (options.integer, options.pad))
		if options.byte_aligned:
			print ('Only byte-aligned values.')
		if results_be:
			print ('Big-endian offsets: %r' % (results_be,))
		else:
			print ('No big-endian offsets found.')
	
		if results_le:
			print ('Little-endian offsets: %r' % (results_le,))
		else:
			print ('No little-endian offsets found.')

		if results_be_revbits:
			print ('Big-endian bit-reversed offsets: %r' % (results_be_revbits,))
		else:
			print ('No big-endian bit-reversed offsets found.')

		if results_le_revbits:
			print ('Little-endian bit-reversed offsets: %r' % (results_le_revbits,))
		else:
			print ('No little-endian bit-reversed offsets found.')
		
		print ("")



if __name__ == '__main__':
	main()

