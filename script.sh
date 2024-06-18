#!/bin/bash

# This script will create the jar files and move the jar files in specific destination/folder.
# It will reduce the time for moving the files manually after creating the jar file.

FAILED_MESSAGE="Maven Build Failed"
PATH_OPEN_FAILED="Failed to open the rest folder."
MOVING_FAILED="Failed to move jar files"
FILE_MOVED="File moved"

# Directory to search and the target directory to move the file to
SEARCH_DIR="./target"
TARGET_DIR="../keycloak-24.0.3/providers"
FILE_NAME="*.jar"


if ! mvn clean package; then
  echo "FAILED_MESSAGE"
  exit 1
fi

echo "Searching in directory: $SEARCH_DIR"
echo "Target directory: $TARGET_DIR"
echo "File name pattern: $FILE_NAME"

# Find the file and move it to the target directory
found_files=$(find "$SEARCH_DIR" -type f -name "$FILE_NAME")

move_files() {
  local search_dir=$1
  local target_dir=$2
  local file_name=$3

  echo "Searching in directory: $search_dir"
  echo "Target directory: $target_dir"
  echo "File name pattern: $file_name"

  found_files=$(find "$search_dir" -type f -name "$file_name")

  if [ -z "$found_files" ]; then
    echo "No files found matching the pattern: $file_name"
  else
    echo "Files found: $found_files"
    for file in $found_files; do
      if mv -f "$file" "$target_dir"; then
        echo "Moved file: $file to $target_dir"
      else
        echo "Failed to move file: $file"
        exit 1
      fi
    done
  fi
}

# Moving jar files to the providers folder
if ! move_files "$SEARCH_DIR" "$TARGET_DIR" "$FILE_NAME"; then
  echo "$MOVING_FAILED"
  exit 1
  else
    echo "$FILE_MOVED"
fi

cd ..

if ! cd keycloak-24.0.3; then
  echo "Failed to open keycloak folder"
  exit 1
fi

if ! bin/kc.sh start-dev; then
  echo "kc build failed"
  exit 1
fi

# Setting the path for authenticators to start again for next usage
cd ..
cd rest || echo "Unable to locate the authenticators folder"
