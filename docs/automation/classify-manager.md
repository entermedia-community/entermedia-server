# Classify Manager

## Overview

The Classify Manager is a tool for organizing and categorizing assets within the EnterMedia system.

## Features

- Automatic asset classification
- Custom classification rules
- Batch processing capabilities
- Integration with metadata management

## Usage

### Basic Classification

```bash
# Example classification command
classify-manager --input /path/to/assets --rules /path/to/rules.json
```

### Configuration

Configure classification rules in your settings:

```json
{
    "rules": [
        {
            "pattern": "*.jpg",
            "category": "images"
        }
    ]
}
```

## API Reference

### classify()

Classifies an asset based on defined rules.

**Parameters:**
- `asset` - The asset to classify
- `rules` - Classification rules to apply

**Returns:** Classification result object

## See Also

- [Asset Management](../asset-management.md)
- [Metadata Guide](../metadata-guide.md)