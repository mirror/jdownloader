#! /usr/bin/perl -w

# This script tries to find translatable strings in the source code
# It does not detect all strings!
# Must be called from main SVN directory or path needs to be passed as argument.

use File::Find;
use strict;
use utf8;

my $debug = 1; # set this to 1, 2 or 3 to get verbose output.
my $path = $ARGV[1] || "src/jd";
my $rpath = $ARGV[1] || "ressourcen/jd/languages/";

main();

sub loadres
{
  my %res;
  my @all = map {/$rpath(.*?)\.loc/;$1} glob("$rpath*.loc");

  foreach my $l (@all)
  {
    my $file = "$rpath$l.loc";
    open FILE,"<",$file or die "Can't open $file\n";
    while(my $line = <FILE>)
    {
      if($line =~ /^(.*?)\s*=\s*(.*$)$/)
      {
        print STDERR "Mismatch for lang $l string $1: '$2' != '$res{$1}{$l}'\n" if($res{$1}{$l});
        $res{$1}{$l} = $2;
      }
      elsif($debug)
      {
        print STDERR "Error loading lang $l line: $line";
      }
    }
    close FILE;
  }
  return %res;
}

sub findfiles
{
  my $dir = $_[0] ? $_[0] : 'src';
  my @findres = ();
  File::Find::find({follow => 1, no_chdir => 1, wanted => sub
  {
    push(@findres, $_) if(-f _ && $_ =~ /\.java/ && !($_ =~/\.svn/));
  }}, $dir);
  return sort @findres;
}

sub check
{
  my ($o, $v, $res, $file) = @_;

  if($o =~ /"/)
  {
    print STDERR "Can't parse following expression in $file: $o = $v\n" if $debug >= 3;
    return;
  }

  my $k = lc($o);
  print STDERR "Mismatch for $k: '$v' != '$res->{$k}'\n" if($res->{$k} && $res->{$k} ne $v);
  print STDERR "Uppercase string $o\n" if $debug >= 2 && $k ne $o;
  $res->{$k} = $v;
}

sub checktb
{
  my ($o, $res, $file) = @_;
  check("gui.menu.$o.name", $o, $res, $file);
  check("gui.menu.$o.tooltip", $o, $res, $file);
  check("gui.menu.$o.mnem", "-", $res, $file);
  check("gui.menu.$o.accel", "-", $res, $file);
}

sub main
{
  my %res;
  foreach my $file (findfiles($path))
  {
    my $prefix;
    my $toolbar;
    my $menu;
    my $istrd;
    my $lastline = "";
    open FILE,"<",$file or die;
    my $name = $file; $name =~ s/.*?src\///; $name =~ s/\//./g; $name =~ s/\.java$//;
    my $sname = $name; $sname =~ s/^.*\.//;
    print STDERR "Parsing $file ($name, $sname)\n" if $debug >= 3;
    while(my $line = <FILE>)
    {
      $line = "$line$lastline"; $lastline = "";
      $line =~ s/^\s*\/\/.*$//; # remove comments
      $line =~ s/^\s*\*.*$//; # remove comments
      $line =~ s/JDL.LOCALE_PARAM//; # prevent false warning in code below
      $line =~ s/this\.getClass\(\)\.getName\(\) \+ "/"$name/g;
      $line =~ s/this\.getClass\(\)\.getSimpleName\(\)/"$sname"/g;
      $prefix = $1 if($line =~ s/[A-Z]+_PREFIX\s*=\s*"(.*?)"//);
      $istrd = $1 if($line =~ s/INFO_STRING_DEFAULT\s*=\s*(".*?")//);
      $toolbar = 1 if($line =~ /extends\s+(ToolBar|Threaded|Menu)Action/);
      $menu = 1 if($line =~ /extends\s+JStartMenu/);
      $line =~ s/INFO_STRING_DEFAULT/$istrd/g;
      if($line =~ /extends\s+PluginOptional/)
      {
        check($name, $sname, \%res, $file);
      }

      while($line =~ s/JDL\.LF?\("(.*?)",\s*"(.*?)"//)
      {
        check($1, $2, \%res, $file);
      }
      while($prefix && $line =~ s/JDL\.LF?\([A-Z]+_PREFIX\s*\+\s*"(.*?)",\s*"(.*?)"//)
      {
        check("$prefix$1", $2, \%res, $file);
      }
      while($toolbar && $line =~ s/super\("(.*?)",\s*"(.*?)"//)
      {
        checktb($1, \%res, $file);
      }
      while($menu && $line =~ s/super\("(.*?)",\s*"(.*?)"//)
      {
        check($1, "", \%res, $file);
      }
      while($line =~ s/(?:new\s+|\.get)(?:ToolBar|Threaded|Menu)Action\("(.*?)",\s*(?:\d+|"(.*?)")//)
      {
        checktb($1, \%res, $file);
      }
      if($line =~ /JDL\.L/)
      {
        if($line =~ /,\w*$/)
        {
          $lastline = $line;
        }
        elsif($debug >= 3)
        {
          print STDERR "Can't parse following line in $file: $line";
        }
      }
    }
    close FILE;
  }
  my %ref = loadres();
  foreach my $k (sort keys %res)
  {
    my $refen = $ref{$k}{en};
    my $refde = $ref{$k}{de};
    if($refen && $res{$k} eq $refen)
    {
    }
    elsif(!($k =~ /\.(accel|mnem)/) && $refde && $res{$k} eq $refde)
    {
      print STDERR "German reference: $k = $refde".($refen?" ($refen) ":"")."\n";
    }
    elsif($refen)
    {
      print STDERR "Mismatch in langfile for $k: '$refen' != '$res{$k}'\n";
    }
    else
    {
      print STDERR "Missing string in langfile $k: '$res{$k}'\n" if $debug;
    }
    delete $ref{$k};
  }
  if($debug)
  {
    foreach my $k (sort keys %ref)
    {
      my $langs = join(" ", keys %{$ref{$k}});
      print STDERR "Superfluos translation ($langs): $k\n";
    }
  }
  foreach my $k (sort keys %res)
  {
    print "$k = $res{$k}\n";
  }
}
