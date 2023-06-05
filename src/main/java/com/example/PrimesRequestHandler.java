package com.example;

import java.io.IOException;
import java.util.List;

@RestController
public class PrimesRequestHandler {
    PrimeGenerator primeGenerator;

    public PrimesRequestHandler() throws IOException {
        this.primeGenerator = new PrimeGenerator(new AppConfiguration());
    }

    @GetMapping("/primes/{count}")
    public List<Integer> getPrimes(@PathVariable("count") int count) throws IOException {
        return this.primeGenerator.calculatePrime(count);
    }

    @GetMapping("/nth-prime/{nth}")
    public Integer getNthPrime(@PathVariable("nth") int count) throws IOException {
        return this.primeGenerator.nthPrime(count);
    }

    @GetMapping("/prime-range/{first}/{last}")
    public List<Integer> getPrimeRange(@PathVariable("first") int first,
                                       @PathVariable("last") int last) throws IOException {
        return this.primeGenerator.primeRange(first, last);
    }
}
