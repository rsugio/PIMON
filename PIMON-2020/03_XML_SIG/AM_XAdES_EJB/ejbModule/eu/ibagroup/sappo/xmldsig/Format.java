package eu.ibagroup.sappo.xmldsig;

public enum Format {
	XAdES ("XAdES v1.3.2"),
	JSR105 ("XMLDSig | JSR105 Provider")
	;
	
	private final String description;
	private Format (String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return this.description;
	}
	

}
