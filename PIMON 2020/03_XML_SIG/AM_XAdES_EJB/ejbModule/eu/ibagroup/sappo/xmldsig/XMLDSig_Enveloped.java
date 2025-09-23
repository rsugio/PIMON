package eu.ibagroup.sappo.xmldsig;

import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.TransformService;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;

import org.jcp.xml.dsig.internal.dom.DOMCanonicalizationMethod;
import org.jcp.xml.dsig.internal.dom.DOMTransform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sap.aii.af.service.resource.SAPSecurityResources;
import com.sap.aii.security.lib.KeyStoreManager;
import com.sap.aii.security.lib.PermissionMode;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.security.api.ssf.ISsfProfile;

import xades4j.XAdES4jException;
import xades4j.algorithms.EnvelopedSignatureTransform;
import xades4j.production.BasicSignatureOptions;
import xades4j.production.DataObjectReference;
import xades4j.production.SignatureAppendingStrategies;
import xades4j.production.SignedDataObjects;
import xades4j.production.SigningCertificateMode;
import xades4j.production.XadesBesSigningProfile;
import xades4j.production.XadesSigner;
import xades4j.properties.DataObjectDesc;
import xades4j.providers.KeyingDataProvider;
import xades4j.providers.impl.DirectKeyingDataProvider;
import xades4j.providers.impl.FileSystemKeyStoreKeyingDataProvider;
import xades4j.providers.impl.KeyStoreKeyingDataProvider.KeyEntryPasswordProvider;
import xades4j.providers.impl.KeyStoreKeyingDataProvider.KeyStorePasswordProvider;
import xades4j.providers.impl.KeyStoreKeyingDataProvider.SigningCertSelector;
import xades4j.utils.XadesProfileResolutionException;

public class XMLDSig_Enveloped {

	private AuditAccess audit = null;

	private String messageId = null;

	public String getMessageId() {
		return messageId;
	}

	public AuditAccess getAudit() {
		return audit;
	}

	public XMLDSig_Enveloped(String messageId, AuditAccess audit) {
		this.audit = audit;
		this.messageId = messageId;
	}

	public final static String DIGEST_METHOD = DigestMethod.SHA256;
	public final static String SIGNATURE_METHOD = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

	public final static String XAdES_DEFAULT_PROFILE = "BES";
	public final static String DOC_MIME_TYPE = "application/xml";
	public final static String DOC_ENCODING = "UTF-8";

	
	
	protected void addSignatureJSR105(Document doc, X509Certificate cert, PrivateKey privateKey) {

		String providerName = null;
		Reference reference = null;

		DOMSignContext domSignContext = null;
		XMLSignature xmlSignature = null;

		Element root = doc.getDocumentElement();

		try {

			// Create DOM XMLSignatureFactory
			providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");

			Provider provider = (Provider) Class.forName(providerName).newInstance();

			XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM", provider);
			TransformService ts = TransformService.getInstance(Transform.ENVELOPED, "DOM", provider);
			DOMTransform dt = new DOMTransform(ts);

			// Create Reference for the enveloped document digest DIGEST_METHOD and
			// transformation algorithm ENVELOPED
			reference = xmlSignatureFactory.newReference("", xmlSignatureFactory.newDigestMethod(DIGEST_METHOD, null),
					Collections.singletonList(dt), null, null);

			ArrayList<Reference> references = new ArrayList<Reference>();
			references.add(reference);

			// Create the SignedInfo
			ts = TransformService.getInstance(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, "DOM", provider);
			DOMCanonicalizationMethod cm = new DOMCanonicalizationMethod(ts);
			SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo((CanonicalizationMethod) cm,
					xmlSignatureFactory.newSignatureMethod(SIGNATURE_METHOD, null), references);

			// Create a keyInfo
			KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
			X509Data x509Data = keyInfoFactory.newX509Data(Collections.singletonList(cert));
			KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data), "KeyInfo-Id-1");

			domSignContext = new DOMSignContext(privateKey, root);

			// Create XMLSignature
			xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo, null, "Signature-Id-1", null);

			// Marshal, generate (and sign) the enveloped signature
			xmlSignature.sign(domSignContext);

		} catch (InstantiationException e) {
			addLogMessage(AuditLogStatus.ERROR,
					"XMLDsig_Enveloped.addSignatureJSR105(): InstantiationException! " + e.getLocalizedMessage());
		} catch (IllegalAccessException e) {
			addLogMessage(AuditLogStatus.ERROR,
					"XMLDsig_Enveloped.addSignatureJSR105(): IllegalAccessException! " + e.getLocalizedMessage());
		} catch (ClassNotFoundException e) {
			addLogMessage(AuditLogStatus.ERROR,
					"XMLDsig_Enveloped.addSignatureJSR105(): ClassNotFoundException! " + e.getLocalizedMessage());
		} catch (NoSuchAlgorithmException e) {
			addLogMessage(AuditLogStatus.ERROR,
					"XMLDsig_Enveloped.addSignatureJSR105(): NoSuchAlgorithmException! " + e.getLocalizedMessage());
		} catch (InvalidAlgorithmParameterException e) {
			addLogMessage(AuditLogStatus.ERROR,
					"XMLDsig_Enveloped.addSignatureJSR105(): InvalidAlgorithmParameterException! " + e.getLocalizedMessage());
		} catch (MarshalException e) {
			addLogMessage(AuditLogStatus.ERROR,
					"XMLDsig_Enveloped.addSignatureJSR105(): MarshalException! " + e.getLocalizedMessage());
		} catch (XMLSignatureException e) {
			addLogMessage(AuditLogStatus.ERROR,
					"XMLDsig_Enveloped.addSignatureJSR105(): XMLSignatureException! " + e.getLocalizedMessage());
		}

	}

	protected void addSignatureXAdES(Document doc, X509Certificate cert, PrivateKey privateKey, String profileName) {

		KeyingDataProvider kdp = new DirectKeyingDataProvider(cert, privateKey);

		addSignatureXAdES(doc, kdp, profileName);

	}

	protected void addSignatureXAdES(Document doc, KeyingDataProvider kdp, String profileName) {

		if (!profileName.equalsIgnoreCase(XAdES_DEFAULT_PROFILE)) {
			addLogMessage(AuditLogStatus.ERROR, "XMLDsig_Enveloped.addSignatureXAdES(): unknown XAdES signature profile!");
		}

		Element root = doc.getDocumentElement();

		// OBJ TIMESTAMPS and COMMITMENTS
//      IndividualDataObjsTimeStampProperty dataObjsTimeStamp = new IndividualDataObjsTimeStampProperty();
//      AllDataObjsCommitmentTypeProperty globalCommitment = AllDataObjsCommitmentTypeProperty.proofOfApproval();
//      CommitmentTypeProperty commitment = (CommitmentTypeProperty)CommitmentTypeProperty.proofOfCreation();
//		DataObjectDesc referenceObject = new DataObjectReference("").withTransform( new EnvelopedSignatureTransform() ).withDataObjectFormat(new DataObjectFormatProperty(DOC_MIME_TYPE));
//		DataObjectDesc referenceObject = new DataObjectReference("").withTransform( new XPathTransform("not(ancestor-or-self::ds:Signature)") ).withDataObjectFormat(new DataObjectFormatProperty(DOC_MIME_TYPE));
		
		DataObjectDesc referenceObject = new DataObjectReference("").withTransform(new EnvelopedSignatureTransform());
		SignedDataObjects signedDataObjects = new SignedDataObjects().withSignedDataObject(referenceObject);

		XadesSigner signer;
		try {
			signer = new XadesBesSigningProfile(kdp).withBasicSignatureOptions(new BasicSignatureOptions().includeSigningCertificate(SigningCertificateMode.FULL_CHAIN)).newSigner();
			signer.sign(signedDataObjects, root, SignatureAppendingStrategies.AsLastChild);

			// adding debug info
			// String xmlString = XMLUtils.printXML(root.getElementsByTagName("ds:Signature").item(0), " ");
			// System.out.println(xmlString);

		} catch (XadesProfileResolutionException e) {
			addLogMessage(AuditLogStatus.ERROR,
					"XMLDsig_Enveloped.addSignatureXAdES(): XadesProfileResolutionException! "
							+ e.getLocalizedMessage());
		} catch (XAdES4jException e) {
			addLogMessage(AuditLogStatus.ERROR,
					"XMLDsig_Enveloped.addSignatureXAdES(): XAdES4jException! " + e.getLocalizedMessage());
		}

	}

	protected void signWithXades(Document doc, String pfxKeystorePath, String password, boolean certificateFullChain) throws Exception {

//		KeyingDataProvider kdp = new FileSystemKeyStoreKeyingDataProvider(keyStoreType, keyStorePath, certificateSelector, keyStorePasswordProvider, entryPasswordProvider, returnFullChain)	
//		KeyingDataProvider kdp = new PKCS11KeyStoreKeyingDataProvider(nativeLibraryPath, providerName, certificateSelector, keyStorePasswordProvider, entryPasswordProvider, returnFullChain)

		KeyingDataProvider kdp = new FileSystemKeyStoreKeyingDataProvider("pkcs12", pfxKeystorePath,
				new SigningCertSelector() {

					@Override
					public X509Certificate selectCertificate(List<X509Certificate> availableCertificates) {
						X509Certificate certificate = availableCertificates.get(0);
		
						try {
							logUserCertificateAndCheckValidity(certificate);
						} catch (Exception e) {
							addLogMessage(AuditLogStatus.ERROR, e.getCause() + ": " + e.getLocalizedMessage());
						}
		
						return certificate;
					}
				}, new KeyStorePasswordProvider() {
		
					@Override
					public char[] getPassword() {
						return password.toCharArray();
					}
				}, new KeyEntryPasswordProvider() {
		
					@Override
					public char[] getPassword(String entryAlias, X509Certificate entryCert) {
						return password.toCharArray();
					}
		
				}, certificateFullChain);

		addSignatureXAdES(doc, kdp, XAdES_DEFAULT_PROFILE);

	}
	

	protected void sign(Format xmldsigFormat, Document doc, InputStream pfxKeystore, String password) throws Exception {

		X509Certificate cert = null;
		PrivateKey privateKey = null;

		// use .pfx file as a local keystore
		KeyStore pfx = KeyStore.getInstance("pkcs12", "SunJSSE");
		pfx.load(pfxKeystore, password.toCharArray());

		// List the aliases
		Enumeration<String> aliases = pfx.aliases();
		String ks_alias = null;
		for (; aliases.hasMoreElements();) {
			ks_alias = (String) aliases.nextElement();
		}

		cert = (X509Certificate) pfx.getCertificate(ks_alias);
		privateKey = (PrivateKey) pfx.getKey(ks_alias, password.toCharArray());

		logUserCertificateAndCheckValidity(cert);

		addSignature(xmldsigFormat, doc, cert, privateKey);

	}

	protected void sign(Format xmldsigFormat, Document doc, String piKeyStoreView, String privateKeyName, String password) throws Exception {

		KeyStoreManager keyManager;

		try {

			keyManager = SAPSecurityResources.getInstance().getKeyStoreManager(PermissionMode.SYSTEM_LEVEL,
					new String[] { "sap.com/com.sap.aii.adapter.sample.ra" });
			KeyStore keyStore = keyManager.getKeyStore(piKeyStoreView);

			if (keyStore == null)
				addLogMessage(AuditLogStatus.ERROR,
						"XMLDsig_Enveloped.sign(): can't access keystore " + piKeyStoreView);

			ISsfProfile keyProfile = keyManager.getISsfProfile(keyStore, privateKeyName, password);

			if (keyProfile == null)
				addLogMessage(AuditLogStatus.ERROR, "XMLDsig_Enveloped.sign(): can't access key " + privateKeyName);

			X509Certificate cert = keyProfile.getCertificate();
			PrivateKey key = keyProfile.getPrivateKey();

			logUserCertificateAndCheckValidity(cert);

			addSignature(xmldsigFormat, doc, cert, key);

		} catch (KeyStoreException e) {
			addLogMessage(AuditLogStatus.ERROR, "KeyStoreException while acccesing SAP PO key storage " + piKeyStoreView + ": " + e.getMessage());
		}

	}

	
	private void addSignature(Format xmldsigFormat, Document doc, X509Certificate cert, PrivateKey privateKey) {
		
		addLogMessage(AuditLogStatus.SUCCESS, "Signature format: " + xmldsigFormat.getDescription());
		addLogMessage(AuditLogStatus.SUCCESS, "Signature method: " + SIGNATURE_METHOD);
		addLogMessage(AuditLogStatus.SUCCESS, "Digest method: " + DIGEST_METHOD);
		addLogMessage(AuditLogStatus.SUCCESS, "Signature type: " + SignatureType.ENVELOPED);
		
		switch (xmldsigFormat) {
			case XAdES: 
						{
							addLogMessage(AuditLogStatus.SUCCESS, "XAdES default profile: " + XMLDSig_Enveloped.XAdES_DEFAULT_PROFILE);
							addSignatureXAdES(doc, cert, privateKey, XAdES_DEFAULT_PROFILE);
							break;
						}
			case JSR105: addSignatureJSR105(doc, cert, privateKey); break;
		}
		
	}
	
	
	
	private void logUserCertificateAndCheckValidity(X509Certificate cert) throws Exception {
		addLogMessage(AuditLogStatus.SUCCESS, "User certificate details" + "\n" + 
											  "SubjectDN: " + cert.getSubjectDN() + "\n" +
											  "Certificate type: " + cert.getType() + " version " + cert.getVersion() + "\n" + 
											  "Certificate expiration date: " + cert.getNotAfter()
				     );
		
		cert.checkValidity();
		
	}
	

	protected void addLogMessage(AuditLogStatus status, String message) {

		if (getAudit() == null) {
			return;
		}
		/*
		 * String uuidTimeLow = messageId.substring(0, 8); String uuidTimeMid =
		 * messageId.substring(8, 12); String uuidTimeHighAndVersion =
		 * messageId.substring(12, 16); String uuidClockSeqAndReserved =
		 * messageId.substring(16, 18); String uuidClockSeqLow = messageId.substring(18,
		 * 20); String uuidNode = messageId.substring(20, 32); String msgUUID =
		 * uuidTimeLow + DASH + uuidTimeMid + DASH + uuidTimeHighAndVersion + DASH +
		 * uuidClockSeqAndReserved + uuidClockSeqLow + DASH + uuidNode;
		 */

		MessageKey msgKey = new MessageKey(getMessageId(), MessageDirection.OUTBOUND);

		getAudit().addAuditLogEntry(msgKey, status, message);

	}

}
