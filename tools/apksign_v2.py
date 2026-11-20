#!/usr/bin/env python3
"""APK Signature Scheme v2 signer, used because this sandbox has no JDK.

The release key never leaves the machine it is used on: this tool signs the
zipaligned APK produced by build-current.yml, and the publish workflow verifies
the result with the real `apksigner` (signer cert digest must be the pinned
Kuro Anime key) before anything is uploaded.

minSdk of the app is 26, so v2 alone is enough - v1 (JAR) signing only matters
for Android 6 and older.

Usage:
    python3 tools/apksign_v2.py <keystore.p12> <password> <in.apk> <out.apk>

Format reference: AOSP "APK Signature Scheme v2" (block id 0x7109871a).
"""
import hashlib
import struct
import sys

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.serialization import pkcs12

V2_BLOCK_ID = 0x7109871A
SIG_ALG_RSA_PKCS1_SHA256 = 0x0103
DIGEST_ALG_SHA256 = 0x0101
CHUNK = 1024 * 1024
MAGIC = b"APK Sig Block 42"

EOCD_SIG = b"PK\x05\x06"
CD_SIG = b"PK\x01\x02"


def u32(n):
    return struct.pack("<I", n)


def u64(n):
    return struct.pack("<Q", n)


def lp(data):
    """uint32 length prefix."""
    return u32(len(data)) + data


def find_eocd(data):
    start = max(0, len(data) - 66560)  # 64 KiB comment + 22 byte record
    pos = data.rfind(EOCD_SIG, start)
    while pos >= 0:
        if pos + 22 <= len(data):
            comment_len = struct.unpack("<H", data[pos + 20:pos + 22])[0]
            if pos + 22 + comment_len == len(data):
                return pos
        pos = data.rfind(EOCD_SIG, start, pos)
    raise SystemExit("EOCD not found")


def eocd_fields(eocd):
    (cd_size, cd_offset) = struct.unpack("<II", eocd[12:20])
    comment_len = struct.unpack("<H", eocd[20:22])[0]
    return cd_size, cd_offset, comment_len


def section_digest(data, cd_offset, eocd):
    """SHA-256 over the three sections, chunked as the v2 spec requires."""
    cd_size, _, _ = eocd_fields(eocd)
    cd = data[cd_offset:cd_offset + cd_size]
    # EOCD as it will look after insertion: central directory offset points at
    # the APK Signing Block, which starts where the CD used to start.
    eocd_patched = bytearray(eocd)
    struct.pack_into("<I", eocd_patched, 16, cd_offset)
    chunk_digests = []
    for section in (data[:cd_offset], cd, bytes(eocd_patched)):
        for i in range(0, len(section), CHUNK):
            part = section[i:i + CHUNK]
            chunk_digests.append(hashlib.sha256(b"\xa5" + u32(len(part)) + part).digest())
    # The content digest is computed over: byte 0x5a, the total number of chunks
    # (uint32 little-endian), then the digests of all chunks in order.
    return hashlib.sha256(b"\x5a" + u32(len(chunk_digests)) + b"".join(chunk_digests)).digest()


def build_signer(cert_der, spki_der, key, digest):
    # digests: length-prefixed sequence of length-prefixed digest records
    digest_record = u32(SIG_ALG_RSA_PKCS1_SHA256) + lp(digest)
    digests_seq = lp(digest_record)
    certificates_seq = lp(cert_der)
    additional_attributes_seq = b""
    signed_data = lp(digests_seq) + lp(certificates_seq) + lp(additional_attributes_seq)

    signature = key.sign(signed_data, padding.PKCS1v15(), hashes.SHA256())
    signature_record = u32(SIG_ALG_RSA_PKCS1_SHA256) + lp(signature)
    signatures_field = lp(lp(signature_record))

    signer = lp(signed_data) + signatures_field + lp(spki_der)
    v2_value = lp(lp(signer))
    # AOSP: the pair's uint64 length covers the ID and the value, not itself.
    pair = u32(V2_BLOCK_ID) + v2_value
    return u64(len(pair)) + pair


def sign(in_path, out_path, key, cert_der, spki_der):
    data = open(in_path, "rb").read()
    eocd_pos = find_eocd(data)
    eocd = data[eocd_pos:]
    cd_size, cd_offset, comment_len = eocd_fields(eocd)
    if cd_offset + cd_size != eocd_pos:
        raise SystemExit("APK has trailing data between the central directory and the EOCD")

    digest = section_digest(data, cd_offset, eocd)
    pairs = build_signer(cert_der, spki_der, key, digest)
    block_size = len(pairs) + 16 + 8
    block = u64(block_size) + pairs + u64(block_size) + MAGIC

    new_eocd = bytearray(eocd)
    struct.pack_into("<I", new_eocd, 16, cd_offset + len(block))

    with open(out_path, "wb") as fh:
        fh.write(data[:cd_offset])
        fh.write(block)
        fh.write(data[cd_offset:cd_offset + cd_size])
        fh.write(bytes(new_eocd))
    return digest


def load_key(p12_path, password):
    blob = open(p12_path, "rb").read()
    key, cert, _ = pkcs12.load_key_and_certificates(blob, password.encode())
    if key is None or cert is None:
        raise SystemExit("keystore could not be opened with that password")
    cert_der = cert.public_bytes(serialization.Encoding.DER)
    spki = key.public_key().public_bytes(
        serialization.Encoding.DER,
        serialization.PublicFormat.SubjectPublicKeyInfo,
    )
    return key, cert_der, spki


def main(argv):
    if len(argv) != 5:
        print(__doc__)
        return 2
    _, p12_path, password, in_path, out_path = argv
    key, cert_der, spki = load_key(p12_path, password)
    digest = sign(in_path, out_path, key, cert_der, spki)
    print("content digest:", digest.hex())
    print("signed:", out_path)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
