i := 256.0;
j := 2.0e0;
k := -3;
l := -3.14;
m := -3*6-2;
write('Negatives: ');
write(k);
write(', ');
write(l);
write(' and ');
write(m);
writeln;
writeln;
write('Division:');
writeln;
write(i);
writeln;
while i > 2.0 do (
  i := i/j;
  write(i);
  writeln
)