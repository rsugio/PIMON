package eu.ibagroup.sappo.xmldsig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * Misc XML utils
 *
 */
public class XMLUtils {
	
    /**
     * Converts string representation of XML into Document
     * @param str              String representation of XML
     * @return                 DOM object
     * @throws IOException     I/O exception
     */
    public static Document stringToDoc(String str) throws IOException {
    	if (str != null && str.length() != 0) {
    		try {    			
    			Reader reader = new StringReader(str);
    			
    			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    			Document doc = db.parse(new InputSource(reader));
    			    			
    			reader.close();
    	        
    			return doc;
    		} catch (Exception ex) {
    			throw new IOException(String.format("Error converting from string to doc %s", ex.getMessage()));
    		}
    	} else {
    		throw new IOException("Error - could not convert empty string to doc");
    	}
	}

    /**
     * Converts doc to String
     * @param dom            DOM object to convert
     * @return               XML as String
     */
    public static String docToString(Document dom) {
		return XMLUtils.docToString1(dom);
	}


	public static String docToString1(Document dom) {
		StringWriter sw = new StringWriter();
		return sw.toString();
	}

	/**
	 * Convert a DOM tree into a String using transform
     * @param domDoc                  DOM object
     * @throws java.io.IOException    I/O exception
     * @return                        XML as String
     */
	public static String docToString2(Document domDoc) throws IOException {
		try {
			TransformerFactory transFact = TransformerFactory.newInstance();
			Transformer trans = transFact.newTransformer();
			trans.setOutputProperty(OutputKeys.INDENT, "no");
			StringWriter sw = new StringWriter();
			Result result = new StreamResult(sw);
			trans.transform(new DOMSource(domDoc), result);
			return sw.toString();
		} catch (Exception ex) {
			throw new IOException(String.format("Error converting from doc to string %s", ex.getMessage()));
		}
	}
	
	
	// create DOM structure from XML
	protected static Document getDOMDocumentFromXML(InputStream input) {

		Document doc = null;

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(input);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return doc;

	}

	// convert W3C XML Document into byte array 
	protected static byte[] documentToBytes(Document doc) {
		try {
			Source source = new DOMSource(doc);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Result result = new StreamResult(out);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.transform(source, result);

			return out.toByteArray();

		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	protected static void saveDocumentToXML(Document doc, String fileName) {
		try {

			OutputFormat of = new OutputFormat(doc, "UTF-8", false);
			of.setOmitXMLDeclaration(false);
			
			// use specific Xerces class to write DOM-data to a file:
			XMLSerializer serializer = new XMLSerializer();
			
			serializer.setOutputCharStream(new java.io.FileWriter(fileName));
			serializer.setOutputFormat(of);
			serializer.serialize(doc);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	
	protected static String removeXmlStringNamespaceAndPreamble(String xmlString) {
		  return xmlString.replaceAll("(<\\?[^<]*\\?>)?", ""). /* remove preamble */
		  replaceAll("xmlns.*?(\"|\').*?(\"|\')", "") /* remove xmlns declaration */
		  .replaceAll("(<)(\\w+:)(.*?>)", "$1$3") /* remove opening tag prefix */
		  .replaceAll("(</)(\\w+:)(.*?>)", "$1$3"); /* remove closing tags prefix */
	}
	
	

	
	protected static String parseXmlWithXPath(String xpathString, InputStream input) {
		
		XPathFactory  factory = XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		String result = "";
		InputSource inputSource = new InputSource(input);
		
		try {
			XPathExpression  xPathExpression = xPath.compile(xpathString);
			result = xPathExpression.evaluate(inputSource);
			
			
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		
		return result;
		
	}

	
	
	
	protected static void printNodeInTraceLog(Node rootNode, String spacer) {
		System.out.println(spacer + rootNode.getNodeName() + " : " + rootNode.getNodeValue());
		NodeList nl = rootNode.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++)
			printNodeInTraceLog(nl.item(i), spacer + "   ");
	}
	
	
	
	
	private static boolean skipNL = false;
	
	protected static String printXML(Node rootNode, String tab) {
	    String print = "";
	    if(rootNode.getNodeType()==Node.ELEMENT_NODE) {
	        print += "\n"+tab+"<"+rootNode.getNodeName()+">";
	    }
	    NodeList nl = rootNode.getChildNodes();
	    if(nl.getLength()>0) {
	        for (int i = 0; i < nl.getLength(); i++) {
	            print += printXML(nl.item(i), tab+"  ");    // \t
	        }
	    } else {
	        if(rootNode.getNodeValue()!=null) {
	            print = rootNode.getNodeValue();
	        }
	        skipNL = true;
	    }
	    if(rootNode.getNodeType()==Node.ELEMENT_NODE) {
	        if(!skipNL) {
	            print += "\n"+tab;
	        }
	        skipNL = false;
	        print += "</"+rootNode.getNodeName()+">";
	    }
	    return(print);
	}

}
