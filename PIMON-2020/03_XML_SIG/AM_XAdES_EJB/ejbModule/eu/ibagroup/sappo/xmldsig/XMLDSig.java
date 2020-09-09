package eu.ibagroup.sappo.xmldsig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;

import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.w3c.dom.Document;

import com.sap.aii.af.lib.mp.module.Module;
import com.sap.aii.af.lib.mp.module.ModuleContext;
import com.sap.aii.af.lib.mp.module.ModuleData;
import com.sap.aii.af.lib.mp.module.ModuleException;
import com.sap.aii.af.lib.mp.module.ModuleHome;
import com.sap.aii.af.lib.mp.module.ModuleLocal;
import com.sap.aii.af.lib.mp.module.ModuleLocalHome;
import com.sap.aii.af.lib.mp.module.ModuleRemote;
import com.sap.engine.interfaces.messaging.api.Message;
import com.sap.engine.interfaces.messaging.api.MessageDirection;
import com.sap.engine.interfaces.messaging.api.MessageKey;
import com.sap.engine.interfaces.messaging.api.MessagePropertyKey;
import com.sap.engine.interfaces.messaging.api.Payload;
import com.sap.engine.interfaces.messaging.api.PublicAPIAccessFactory;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditAccess;
import com.sap.engine.interfaces.messaging.api.auditlog.AuditLogStatus;
import com.sap.engine.interfaces.messaging.api.exception.InvalidParamException;
import com.sap.engine.interfaces.messaging.api.exception.MessagingException;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * Session Bean implementation class XMLDSig
 */

@Stateless(name="AM_XMLDSig")
@Local(value={ModuleLocal.class})
@Remote(value={ModuleRemote.class})
@LocalHome(value=ModuleLocalHome.class)
@RemoteHome(value=ModuleHome.class)
public class XMLDSig implements Module {

	// adapter module parameters
	public static final String KEYSTORE_TYPE = "keystore.type"; // keystore type name
	public static final String KEYSTORE_VIEW = "po.keystore"; // keystore view name
	public static final String KEYSTORE_PO_PWD = "password"; // password for po keystore entry
//	public static final String ALIAS = "key.alias"; // keystore entry alias name, normally key.alias == signer.id if 1 user has only own private key
	public static final String DC_PROPERTY = "signer.id"; // dynamic configuration property; parameter value format -> "dc|<namespace>|<property_name>" 
	public static final String XML_SIGNATURE_FORMAT = "signature.format"; // XAdES, JSR105
	
	public static final String ADAPTER_MODULE_NAME = "AM_XMLDSig"; 
	public static final String DEFAULT_SIGNATURE_FORMAT = Format.XAdES.toString();
	
	
	public static final String PFX_KEYSTORE_LOCAL = "keystore/dmanko.pfx";
	public static final String PFX_KEYSTORE_PASS = "UTEST_PASSWROD";

	private AuditAccess audit = null;
	
    /**
     * Default constructor. 
     */
    public XMLDSig() {
        // TODO Auto-generated constructor stub
    }
    
    
    @PostConstruct
    public void initializeResorces(){
    	
		try {
			this.audit = PublicAPIAccessFactory.getPublicAPIAccess().getAuditAccess();
		} catch (MessagingException e) {
			throw new RuntimeException("MessagingException while getting the AuditAccess object: " + e.getLocalizedMessage());
		}
		
    }
    
    
    @PreDestroy
    public void releaseResources() {
    	
    }

	@Override
	public ModuleData process(ModuleContext moduleContext, ModuleData inputModuleData) throws ModuleException {
		
		Instant start = Instant.now();
		
		// getting module parameters
		String keystoreType =  moduleContext.getContextData(KEYSTORE_TYPE);
		String keystoreView =  moduleContext.getContextData(KEYSTORE_VIEW);
		String keystorePoPWD =  moduleContext.getContextData(KEYSTORE_PO_PWD);
//		String alias = moduleContext.getContextData(ALIAS);
		
		if (keystoreType == null) {
			keystoreType = KeystoreType.JAVA_FILE.toString();
		}
		
		if (keystoreType == KeystoreType.PO_KEYSTORAGE.toString() && (keystoreView == null || keystorePoPWD == null)) {
			throw new ModuleException("Mandatory parameter \"" + (keystoreView == null ? KEYSTORE_VIEW : KEYSTORE_PO_PWD)  + "\" not specified");
		}
		
		String dc_string = moduleContext.getContextData(DC_PROPERTY);
		if (dc_string == null) {
			throw new ModuleException("Mandatory parameter \"" + DC_PROPERTY + "\" not specified ");
		}
		List<String> dc_string_parts = Arrays.asList(dc_string.split("\\|"));
		
		if (!dc_string_parts.get(0).equalsIgnoreCase("dc")) {
			throw new ModuleException("Mandatory parameter \"" + DC_PROPERTY + "\" format: dc|<namespace>|<property_name> ");
		}
		

		String sig_format = moduleContext.getContextData(XML_SIGNATURE_FORMAT);
		
		if (sig_format == null ) {
			sig_format = DEFAULT_SIGNATURE_FORMAT;
		}
		// load XML payload from the message
		Message msg = (Message)inputModuleData.getPrincipalData();
		// message key for logging
		MessageKey amk = new MessageKey(msg.getMessageId(), MessageDirection.OUTBOUND);
		
		String userId =  getAttributeFromDC(msg, dc_string_parts.get(1), dc_string_parts.get(2));
		
		if (userId == null) {
			audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "signer ID is not provided in Dynamic Configuration, check DC attribute " + DC_PROPERTY);
		}
		
		Payload payload = msg.getMainPayload();
		InputStream is = payload.getInputStream();

		// say hello
		audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "<====================== " + ADAPTER_MODULE_NAME +  "======================> ");
		
		KeystoreType KT = KeystoreType.valueOf(keystoreType);
		
		byte [] keystore = null;
		
		String password = null;
		
		// get DOM structure of XML message
		Document xmldoc = XMLUtils.getDOMDocumentFromXML(is);
		xmldoc.normalizeDocument();

		XMLDSig_Enveloped xmldsig = new XMLDSig_Enveloped(msg.getMessageId(), audit);

		try {
			
			switch (KT) {
				case JAVA_FILE: {
					keystore = eu.ibagroup.sappo.keystore.LocalKeyStore.secrets.get(userId);
					
					if (keystore == null) {
						audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "LocalKeystore_PL doesn't contain a private key for specified declarant: " + userId);
					}
					
					// password to access local PFX keystore
					String passwordEncoded = eu.ibagroup.sappo.keystore.LocalKeyStore.passwords.get(userId);
					
					if (passwordEncoded == null) {
						audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "LocalKeystore_PL doesn't contain a password to access PFX keystore for declarant: " + userId);
					}
					
					try {
						password = new String(Base64.decode(passwordEncoded));
					} catch (Base64DecodingException e1) {
						audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "Password decoding error for user " + userId);
					}
					InputStream pfxKeystore = new ByteArrayInputStream(keystore);
					
					xmldsig.sign(Format.valueOf(sig_format), xmldoc, pfxKeystore, password);
					
					pfxKeystore.close();
					break;
				}
				case PO_KEYSTORAGE: {
					password = keystorePoPWD;
					xmldsig.sign(Format.valueOf(sig_format), xmldoc, keystoreView, userId, password);
					break;
				}
				case PFX_FILE: {
					xmldsig.signWithXades(xmldoc, PFX_KEYSTORE_LOCAL, PFX_KEYSTORE_PASS, true);
					break;
				}
				default: {
					audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "Unknown keystore type");
				}
			
			}
			
			// write the signed data into the payload
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			OutputFormat of = new OutputFormat(xmldoc, "UTF-8", false);
			of.setOmitXMLDeclaration(false); 		// set XML preamble
			XMLSerializer serializer = new XMLSerializer();
			serializer.setOutputByteStream(baos);
			serializer.setOutputFormat(of);
			serializer.serialize(xmldoc);

			payload.setContent(baos.toByteArray());
			payload.setContentType("application/xml");
			
			
			
		} catch (IOException e) {
			audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "IOException while writting the signed payload; " + e);
			
		} catch (InvalidParamException e) {
			audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, "InvalidParamException while writting the signed payload; " + e);
			
		} catch (Exception e) {
			audit.addAuditLogEntry(amk, AuditLogStatus.ERROR, e.getMessage());
		}
		
		// say goodbye ^_^ and print elapsed time for signature producing
		audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "XML payload signed successfully.");
		
		Instant stop = Instant.now();
		
		long timeElapsed = Duration.between(start, stop).toMillis();
		
		long millis = timeElapsed % 1000;
		long second = (timeElapsed / 1000) % 60;
		long minute = (timeElapsed / (1000 * 60)) % 60;
		long hour = (timeElapsed / (1000 * 60 * 60)) % 24;

		String timeElapsedString = String.format("%02d:%02d:%02d.%d", hour, minute, second, millis);
		
		audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, ADAPTER_MODULE_NAME + "/ signature creation time:  " + timeElapsedString);
		
		audit.addAuditLogEntry(amk, AuditLogStatus.SUCCESS, "<====================== " + ADAPTER_MODULE_NAME +  "======================> ");
		
		return inputModuleData;
		
	}
	
	private static String getAttributeFromDC(Message msg, String dc_namespace, String dc_name) {
		
		MessagePropertyKey mpk = new MessagePropertyKey(dc_name, dc_namespace);
		
		return msg.getMessageProperty(mpk);
		
		
	}

	

}
