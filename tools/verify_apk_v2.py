#!/usr/bin/env python3
"""Independent check of an APK Signature Scheme v2 block.

Recomputes the content digest from the file itself and verifies the RSA
signature and the signer certificate, so a bad signing run is caught even
without a JDK. The publish workflow still runs the real apksigner on the runner.

Usage:
    python3 tools/verify_apk_v2.py <apk> [expected-cert-sha256-hex]
"""
import hashlib
import struct
import sys

from cryptography import x509
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding

MAGIC = b"APK Sig Block 42"
CHUNK = 1024 * 1024


def u32(b, o):
    return struct.unpack_from("<I", b, o)[0]


def u64(b, o):
    return struct.unpack_from("<Q", b, o)[0]


def read_lp(b, o):
    n = u32(b, o)
    return b[o + 4:o + 4 + n], o + 4 + n


def main(argv):
    if len(argv) not in (2, 3):
        print(__doc__)
        return 2
    data = open(argv[1], "rb").read()
    expected = argv[2].replace(":", "").lower() if len(argv) == 3 else None

    block_end = data.rfind(MAGIC)
    if block_end < 0:
        raise SystemExit("no APK Signing Block magic")
    block_end += len(MAGIC)
    size_trailer = u64(data, block_end - 24)
    block_start = block_end - size_trailer - 8
    size_header = u64(data, block_start)
    if size_header != size_trailer:
        raise SystemExit("signing block sizes disagree")

    pairs = data[block_start + 8:block_end - 24]
    o = 0
    v2 = None
    while o < len(pairs):
        pair_size = u64(pairs, o)
        pid = u32(pairs, o + 8)
        value = pairs[o + 12:o + pair_size]
        if pid == 0x7109871A:
            v2 = value
        o += pair_size
    if v2 is None:
        raise SystemExit("no v2 signature block found")

    signers_seq, _ = read_lp(v2, 0)
    signer, _ = read_lp(signers_seq, 0)

    o = 0
    signed_data, o = read_lp(signer, o)
    signatures_field, o = read_lp(signer, o)
    public_key_der, o = read_lp(signer, o)

    signature_record, _ = read_lp(signatures_field, 0)
    alg, = struct.unpack_from("<I", signature_record, 0)
    sig, _ = read_lp(signature_record, 4)

    o = 0
    digests_seq, o = read_lp(signed_data, o)
    certificates_seq, o = read_lp(signed_data, o)
    attributes_seq, o = read_lp(signed_data, o)
    digest_record, _ = read_lp(digests_seq, 0)
    stored_alg, = struct.unpack_from("<I", digest_record, 0)
    stored, _ = read_lp(digest_record, 4)
    cert_der, _ = read_lp(certificates_seq, 0)

    cert = x509.load_der_x509_certificate(cert_der)
    fp = cert.fingerprint(hashes.SHA256()).hex()

    from cryptography.hazmat.primitives.serialization import load_der_public_key
    pub = load_der_public_key(public_key_der)
    pub.verify(sig, signed_data, padding.PKCS1v15(), hashes.SHA256())

    # content digest
    eocd_pos = len(data) - 22 - struct.unpack_from("<H", data, len(data) - 2)[0]
    cd_size, cd_offset = struct.unpack_from("<II", data, eocd_pos + 12)
    if cd_offset != block_start + (block_end - block_start):
        raise SystemExit("EOCD does not point past the signing block")
    cd = data[cd_offset:cd_offset + cd_size]
    eocd = bytearray(data[eocd_pos:])
    struct.pack_into("<I", eocd, 16, block_start)
    chunks = []
    for section in (data[:block_start], cd, bytes(eocd)):
        for i in range(0, len(section), CHUNK):
            part = section[i:i + CHUNK]
            chunks.append(hashlib.sha256(b"\xa5" + struct.pack("<I", len(part)) + part).digest())
    digest = hashlib.sha256(b"".join(chunks)).digest()

    if stored != digest:
        raise SystemExit("content digest mismatch: stored %s, computed %s" % (stored.hex(), digest.hex()))

    print("signature alg : 0x%04x (RSA PKCS#1 v1.5 / SHA-256)" % alg)
    print("digest alg    : 0x%04x" % stored_alg)
    print("content digest: %s" % digest.hex())
    print("subject       : %s" % cert.subject.rfc4514_string())
    print("cert sha256   : %s" % ":".join(fp[i:i + 2] for i in range(0, len(fp), 2)).upper())
    if expected and fp != expected:
        raise SystemExit("signer certificate mismatch: expected %s" % expected)
    print("v2 signature  : OK")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
