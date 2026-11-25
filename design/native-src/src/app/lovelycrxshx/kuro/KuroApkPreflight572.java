package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import java.io.File;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Checks an update file before Kuro hands it to Android's installer.
 *
 * Android's installer remains the final authority: it re-verifies the package and
 * signature itself, and it can still refuse the update. This preflight only stops
 * Kuro from offering a file that is obviously the wrong package, not newer, or signed
 * by a different key.
 */
final class KuroApkPreflight572 {
    enum Status { OK, MISSING, UNREADABLE, WRONG_PACKAGE, NOT_NEWER, SIGNER_MISMATCH, UNPINNED_SIGNER }

    /**
     * The Kuro release key, pinned in the binary. Matching the installed app alone is
     * not enough: on a device where the installed app was replaced, the comparison
     * would happily accept the replacement's key. Every update must carry this one.
     */
    static final String RELEASE_SIGNER = "1d37c3ca28ed80cc484601a7a7907d5c43970a5d57d51aa4e91475a60703e93e";

    static final String PACKAGE = "app.lovelycrxshx.kuro";

    static final class Result {
        final Status status;
        final String packageName;
        final long versionCode;
        final String versionName;
        final Set<String> signers;

        Result(Status status, String packageName, long versionCode, String versionName, Set<String> signers) {
            this.status = status;
            this.packageName = packageName == null ? "" : packageName;
            this.versionCode = versionCode;
            this.versionName = versionName == null ? "" : versionName;
            this.signers = signers == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(new TreeSet<>(signers));
        }

        boolean ok() {
            return status == Status.OK;
        }

        /** Short, bounded text for a notification or dialog. Never shows a path or key. */
        String message() {
            switch (status) {
                case OK: return "The update file is Kuro " + versionName + " (" + versionCode + ") and matches this app's signing identity.";
                case MISSING: return "The downloaded update file is missing.";
                case UNREADABLE: return "The downloaded file is not a readable Android package.";
                case WRONG_PACKAGE: return "That file is a different app (" + packageName + "), not Kuro.";
                case NOT_NEWER: return "That file is Kuro " + versionName + " (" + versionCode + "), which is not newer than the installed version.";
                case SIGNER_MISMATCH: return "That file was signed by a different key, so Android would reject it over the installed Kuro.";
                case UNPINNED_SIGNER: return "That file is not signed by the Kuro release key, so it will not be installed.";
                default: return "The update file could not be checked.";
            }
        }
    }

    private KuroApkPreflight572() {}

    /**
     * Pure decision step, kept separate so it can be tested without a device.
     * Signer sets must match exactly: a different key cannot update this package.
     */
    static Result evaluate(String packageName, long versionCode, String versionName, Set<String> signers,
                           long installedVersionCode, Set<String> installedSigners) {
        if (packageName == null || packageName.isEmpty()) return new Result(Status.UNREADABLE, packageName, versionCode, versionName, signers);
        if (!PACKAGE.equals(packageName)) return new Result(Status.WRONG_PACKAGE, packageName, versionCode, versionName, signers);
        if (installedSigners == null || installedSigners.isEmpty() || signers == null || signers.isEmpty())
            return new Result(Status.UNREADABLE, packageName, versionCode, versionName, signers);
        if (!installedSigners.equals(signers)) return new Result(Status.SIGNER_MISMATCH, packageName, versionCode, versionName, signers);
        if (!signers.contains(RELEASE_SIGNER)) return new Result(Status.UNPINNED_SIGNER, packageName, versionCode, versionName, signers);
        if (versionCode <= installedVersionCode) return new Result(Status.NOT_NEWER, packageName, versionCode, versionName, signers);
        return new Result(Status.OK, packageName, versionCode, versionName, signers);
    }

    /** Inspects an APK file against the currently installed Kuro. */
    static Result inspect(Context context, File apk) {
        Context app = context.getApplicationContext();
        PackageManager pm = app.getPackageManager();
        long installedCode = 0;
        Set<String> installedSigners = Collections.emptySet();
        try {
            PackageInfo self = pm.getPackageInfo(PACKAGE, signingFlags());
            installedCode = versionCode(self);
            installedSigners = digests(signers(self));
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {}
        if (apk == null || !apk.isFile() || apk.length() <= 0) return new Result(Status.MISSING, "", -1, "", null);
        PackageInfo info;
        try {
            info = pm.getPackageArchiveInfo(apk.getAbsolutePath(), signingFlags());
        } catch (RuntimeException e) {
            info = null;
        }
        if (info == null) return new Result(Status.UNREADABLE, "", -1, "", null);
        return evaluate(info.packageName, versionCode(info), info.versionName, digests(signers(info)), installedCode, installedSigners);
    }

    private static int signingFlags() {
        return Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
    }

    @SuppressWarnings("deprecation")
    private static Signature[] signers(PackageInfo info) {
        if (Build.VERSION.SDK_INT >= 28) {
            SigningInfo signing = info.signingInfo;
            if (signing == null) return null;
            return signing.hasMultipleSigners() ? signing.getApkContentsSigners() : signing.getSigningCertificateHistory();
        }
        return info.signatures;
    }

    private static long versionCode(PackageInfo info) {
        if (Build.VERSION.SDK_INT >= 28) return info.getLongVersionCode();
        return info.versionCode;
    }

    private static Set<String> digests(Signature[] signatures) {
        Set<String> out = new TreeSet<>();
        if (signatures != null) {
            for (Signature signature : signatures) {
                if (signature == null) continue;
                String digest = sha256(signature.toByteArray());
                if (!digest.isEmpty()) out.add(digest);
            }
        }
        return out;
    }

    static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Certificate digest of this installation. */
    static String installedSigner(Context context) {
        try {
            PackageInfo self = context.getPackageManager().getPackageInfo(PACKAGE, signingFlags());
            Set<String> all = digests(signers(self));
            return all.isEmpty() ? "" : all.iterator().next();
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return "";
        }
    }

    static long installedVersionCode(Context context) {
        try {
            return versionCode(context.getPackageManager().getPackageInfo(PACKAGE, 0));
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return 0;
        }
    }
}
