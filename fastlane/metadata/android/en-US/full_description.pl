#!/usr/bin/perl --
use strict;
use warnings;
use utf8;
use feature qw(say);

my $file = $0;
$file =~ s/\.pl$/\.txt/;

local $/=undef;
my $text = <DATA>;

$text =~ s/[\x00-\x20]+/ /g;
$text =~ s/\A //;
$text =~ s/ \z//;

$text =~ s/(p|div|ol|ul|li|blockquote|cite|dd|dl|dt)\>\s/$1>/gi;
$text =~ s/br> /br>/g;
$text =~ s/\s<\/(p|br|div|ol|ul|li|blockquote|cite|dd|dl|dt)/<\/$1/gi;

open(my $fh,">:utf8",$file) or die "$file $!";
say $fh $text;
close($fh) or die "$file $!";

# apt-cyg install tidy libtidy5
system qq(tidy -q -e $file);

__DATA__
<p><i>Scheduler</i> is a small app that shows your todo list on your lockscreen.</p>

<p>
Do you <b>always forget things</b> to do or <b>have a lot of them</b> to remember?<br>
Want to <b>easily keep track</b> of them?<br>
Then this app is for you, take <b>full advantage of your lockscreen real estate</b> and view your todo list right on it!<br>
<b>No need to unlock the phone</b>, it's all there!
</p>

<p><br><b>Features:</b></p>
<ul>
<li>Your todo list on the lockscreen</li>
<li>Sync with your system calendars (google, samsung, local)</li>
<li>Configurable per-day wallpapers</li>
<li>Material you theming</li>
<li>Plethora of settings</li>
<li>Low memory and cpu consumption</li>
<li>And more...</li>
</ul>