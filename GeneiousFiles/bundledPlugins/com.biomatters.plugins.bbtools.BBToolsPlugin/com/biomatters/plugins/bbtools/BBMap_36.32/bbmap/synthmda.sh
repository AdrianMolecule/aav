#!/bin/bash
#synthmda in=<infile> out=<outfile>

usage(){
echo "
Written by Brian Bushnell
Last modified October 22, 2015

Description:  Generates synthetic reads following an MDA-amplified single cell's coverage distribution.

Usage:  synthmda.sh in=<reference> out=<reads out file>

Input may be fasta or fastq, compressed or uncompressed.


Optional parameters (and their defaults)

reads=12000000      Generate this many reads.
paired=t            Generate paired reads.
length=150          Reads should be this long.
minlen=4000         Min length of MDA contig.
maxlen=150000       Max length of MDA contig.
cycles=9            Number of MDA cycles; higher is more spiky.
initialratio=1.3    Fraction of genome initially replicated; 
                    lower is more spiky.
ratio=1.7           Fraction of genome replicated per cycle.
refout=null         Write MDA'd genome to this file.
perfect=0           This fraction of reads will be error-free.
amp=200             'amp' flag sent to RandomReads (higher is more spiky).
build=7             Index MDA'd genome in this location.
prefix=null         Generated reads will startIndex with this prefix.
overwrite=t         (ow) Set to false to force the program to abort rather 
                    than overwrite an existing file.

Java Parameters:
-Xmx                This will be passed to Java to set memory usage, overriding the program's automatic memory detection.
                    -Xmx20g will specify 20 gigs of RAM, and -Xmx200m will specify 200 megs.  The max is typically 85% of physical memory.

Please contact Brian Bushnell at bbushnell@lbl.gov if you encounter any problems.
"
}

pushd . > /dev/null
DIR="${BASH_SOURCE[0]}"
while [ -h "$DIR" ]; do
  cd "$(dirname "$DIR")"
  DIR="$(readlink "$(basename "$DIR")")"
done
cd "$(dirname "$DIR")"
DIR="$(pwd)/"
popd > /dev/null

#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/"
CP="$DIR""current/"

z="-Xmx4g"
z2="-Xms4g"
EA="-ea"
set=0

if [ -z "$1" ] || [[ $1 == -h ]] || [[ $1 == --help ]]; then
	usage
	exit
fi

calcXmx () {
	source "$DIR""/calcmem.sh"
	parseXmx "$@"
	if [[ $set == 1 ]]; then
		return
	fi
	freeRam 4000m 80
	z="-Xmx${RAM}m"
	z2="-Xms${RAM}m"
}
calcXmx "$@"

synthmda() {
	if [[ $NERSC_HOST == genepool ]]; then
		module load pigz
	fi
	local CMD="java $EA $z -cp $CP jgi.SynthMDA $@"
	echo $CMD >&2
	eval $CMD
}

synthmda "$@"
