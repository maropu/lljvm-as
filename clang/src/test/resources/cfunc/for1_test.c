#include <math.h>
#include <stdio.h>

long for1_test(long x[], size_t s) {
  long sum = 0;
  for (int i = 0; i < s; i++) {
    sum += x[i];
  }
  return sum;
}
