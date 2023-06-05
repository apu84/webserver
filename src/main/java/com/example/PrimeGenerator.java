package com.example;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.lines;
import static java.nio.file.Paths.get;

public class PrimeGenerator {
    private final AppConfiguration configuration;

    public PrimeGenerator(AppConfiguration configuration) {
        this.configuration = configuration;
    }

    private List<Integer> prime(List<Integer> providedPrimes, Integer count) {
        List<Integer> primeNumbers = new ArrayList<>(count - providedPrimes.size() + 1);
        int lastPrime = providedPrimes.get(providedPrimes.size() - 1);
        primeNumbers.addAll(providedPrimes);
        int primeCount = primeNumbers.size();

        for (int i = lastPrime + 1; primeCount < count; i++) {
            boolean isPrime = true;
            for (int j = 2; j <= Math.sqrt(i); j++) {
                if (i % j == 0) {
                    isPrime = false;
                    break;
                }
            }
            if (isPrime) {
                primeNumbers.add(i);
                primeCount++;
            }
        }
        return primeNumbers;
    }

    private void writePrimesToFile(List<Integer> primes, String file) throws IOException {
        Files.writeString(get(file), buildPrimesString(primes), StandardOpenOption.CREATE);
    }

    private String buildPrimesString(List<Integer> primes) {
        StringBuilder builder = new StringBuilder();
        for (Integer prime : primes) {
            builder.append(prime);
            builder.append("\n");
        }
        return builder.toString();
    }

    private List<Integer> readPrimes(String file) throws IOException {
        try (var primes = lines(get(file), StandardCharsets.UTF_8)) {
            return primes
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
        }
    }

    private List<Integer> addDefaultPrimes() {
        List<Integer> primes = new ArrayList<>();
        primes.add(2);
        primes.add(3);
        return primes;
    }

    public List<Integer> calculatePrime(Integer count) throws IOException {
        var fileName = configuration.getProperty("app.primes.file").toString();
        var savedPrimes = readPrimes(fileName);
        if (savedPrimes.size() == 0) {
            savedPrimes = addDefaultPrimes();
        }
        if (count > savedPrimes.size()) {
            var primes = prime(savedPrimes, count);
            writePrimesToFile(primes, fileName);
            return primes;
        } else {
            return savedPrimes.subList(0, count);
        }
    }

    public Integer nthPrime(Integer count) throws IOException {
        return calculatePrime(count).get(count-1);
    }

    public List<Integer> primeRange(Integer first, Integer last) throws IOException {
        return calculatePrime(last).subList(first, last);
    }
}
