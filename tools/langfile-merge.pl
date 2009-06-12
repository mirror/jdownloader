##!/usr/bin/perl
##
## This is perl, not java.
##  chmod +x langfile-merge.pl
##  ./langfile-merge.pl
##
## Windows: activestate.com

sub help {

print qq|
LangFile-Merge 1.0
langfile-merge.pl -a "lang1.lng" -b "lang2.lng" -c "output.lng"
Values in file2 will overwrite the values of file1, eg.
file1: foo = foo
file2: foo = bar
out  : foo = bar

~scr4ve|;

exit;
}

use Getopt::Long;
use strict; use warnings;
my ($file1,$file2,$outputfile);
GetOptions ("a=s" => \$file1,    # string
            "b=s" => \$file2, # string
		    "c=s" => \$outputfile,) || help();  # string
help() if ((!defined($file1))||(!defined($file2))||(!defined($outputfile)));
my %result;

%result = %{readlangfile($file1,\%result)};
%result = %{readlangfile($file2,\%result)};

open(OUT,'>'.$outputfile) || die "Can not open file $outputfile: $!";
foreach my $key (sort(keys %result)) {
print OUT $key," = ",$result{$key},"\r\n";
}
close(OUT);
print "Success!\n";
sub readlangfile {
my $filename = shift;
my %hash = %{(shift)};
open(IN,'<'.$filename) || die "Can not open file $filename: $!";
while(my $line = <IN>)
{
    if ($line =~ /\s*(\S+)\s*=\s*(.+?)\s*$/)
    {
        $hash{lc($1)}=$2;
    }
	else
	{
	print "Not proceeded: ",$line,"\n";
	}
}
close IN;
return \%hash;
}