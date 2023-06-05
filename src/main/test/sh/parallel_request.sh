#!/bin/bash

# Array of base URLs
base_url="http://localhost:2000/nth-prime"
# Function to generate random number-based URI
generate_random_number() {
  length=$1
  echo $((RANDOM%$length))
}

# Loop through the base URLs and make parallel curl requests with random number-based URI
for i in {1..100}; do
  number=$(generate_random_number 1000)  # Change 1000 to the desired maximum URI length
  url="$base_url/$number"
#  curl -s "$url" -o "/dev/null" &
#  time curl -s "$url"  &
  curl -o /dev/null -s -w 'Total: %{time_total}s\n' "$url" &
done

# Wait for all background processes to finish
wait
