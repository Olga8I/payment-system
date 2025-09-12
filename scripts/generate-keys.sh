#!/bin/bash

set -e

echo " Generating RSA keys for Payment System..."
echo "=============================================="

POS_RESOURCES="../pos-terminal/src/main/resources"
SERVER_RESOURCES="../acquiring-server/src/main/resources"
PRIVATE_KEY="$SERVER_RESOURCES/server-private.pem"
PUBLIC_KEY="$POS_RESOURCES/server-public.pem"

if ! command -v openssl &> /dev/null; then
    echo -e "${RED} OpenSSL is not installed. Please install it first.${NC}"
    exit 1
fi

mkdir -p "$POS_RESOURCES"
mkdir -p "$SERVER_RESOURCES"

if [ -f "$PRIVATE_KEY" ] || [ -f "$PUBLIC_KEY" ]; then
    echo -e "${YELLOW}  Keys already exist.${NC}"
    read -p "Do you want to overwrite them? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Operation cancelled."
        exit 0
    fi
    echo "Overwriting existing keys..."
fi

echo -e "${YELLOW}Generating private key...${NC}"
openssl genpkey \
    -algorithm RSA \
    -out "$PRIVATE_KEY" \
    -pkeyopt rsa_keygen_bits:2048 \
    -aes256 \
    -pass pass:temp_password

echo -e "${YELLOW}Removing password for development...${NC}"
openssl rsa \
    -in "$PRIVATE_KEY" \
    -out "$PRIVATE_KEY" \
    -passin pass:temp_password

echo -e "${YELLOW}Generating public key...${NC}"
openssl rsa \
    -pubout \
    -in "$PRIVATE_KEY" \
    -out "$PUBLIC_KEY"

chmod 644 "$PRIVATE_KEY"
chmod 644 "$PUBLIC_KEY"

echo -e "${YELLOW}Verifying keys...${NC}"
if [ -f "$PRIVATE_KEY" ] && [ -f "$PUBLIC_KEY" ]; then
    echo -e "${GREEN} Keys generated successfully!${NC}"
    echo -e "${GREEN}   Private key: $PRIVATE_KEY${NC}"
    echo -e "${GREEN}   Public key:  $PUBLIC_KEY${NC}"
    echo
    echo -e "${YELLOW}  For production use:${NC}"
    echo -e "${YELLOW}   - Store private key securely${NC}"
    echo -e "${YELLOW}   - Use proper password protection${NC}"
    echo -e "${YELLOW}   - Rotate keys regularly${NC}"
else
    echo -e "${RED} Key generation failed!${NC}"
    exit 1
fi