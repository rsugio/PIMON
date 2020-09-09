package eu.ibagroup.sappo.keystore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;

public class LocalKeyStore {

	// specify a real username
	public static final String PFX_USER_UTEST = "TEST"; 

	public static HashMap<String, byte[]> secrets = new HashMap<String, byte[]>();

	public static HashMap<String, String> passwords = new HashMap<String, String>();

	static {
		secrets.put(PFX_USER_UTEST, LocalKeyStore_UTEST.PFX_BYTES);
		passwords.put(PFX_USER_UTEST, LocalKeyStore_UTEST.PFX_PASS);
	}

	public static void checkCertificatesValidity() {

		secrets.entrySet().stream().forEach(secret -> {

			KeyStore pfx = null;
			InputStream pfxKeystoreInputStream = null;

			try {

				pfxKeystoreInputStream = new ByteArrayInputStream(secret.getValue());

				char[] password = passwords.get(secret.getKey()).toCharArray();

				pfx = KeyStore.getInstance("pkcs12", "SunJSSE");
				pfx.load(pfxKeystoreInputStream, password);

				Enumeration<String> aliases = pfx.aliases();

				String ks_alias = null;
				for (; aliases.hasMoreElements();) {
					ks_alias = (String) aliases.nextElement();

				}

				X509Certificate cert = (X509Certificate) pfx.getCertificate(ks_alias);

				if (cert != null) {
					System.out.println("SubjectDN: " + cert.getSubjectDN());
					System.out.println("Certificate type: " + cert.getType() + " version " + cert.getVersion());
					System.out.println("Serial number: " + cert.getSerialNumber());
					System.out.println("Signing key / certificate expiration date: " + cert.getNotAfter());
					
					String status = (cert.getNotAfter().getTime() > System.currentTimeMillis()) ? "OK": "EXPIRED!!!"; 
					System.out.println("user " + secret.getKey() + " | STATUS: " +  status);
					System.out.println("===================================");
					
				}

			} catch (Exception e) {
				System.out.println(e.getLocalizedMessage());
			}

			finally {

				if (pfxKeystoreInputStream != null) {
					try {
						pfxKeystoreInputStream.close();
					} catch (IOException e) {
						System.out.println(e.getLocalizedMessage());
					}
				}

			}

		});

	}

	public static void releaseObjects() {
		secrets.clear();
		passwords.clear();

		secrets = null;
		passwords = null;

		System.gc();

	}

}
