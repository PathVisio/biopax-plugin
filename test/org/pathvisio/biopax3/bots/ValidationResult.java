package org.pathvisio.biopax3.bots;

import java.io.Serializable;

public class ValidationResult implements Serializable
{
	static final long serialVersionUID = 1L;
	
	ValidationResult (String pwyId, String pwyTitle, String ruleId, String eltId, String ruleDesc, String msg) 
	{ 
		this.pwyId = pwyId; this.ruleId = ruleId; this.eltId = eltId;
		this.ruleDesc = ruleDesc;
		this.msg = msg;
		this.pwyTitle = pwyTitle;
	}
	
	final String pwyId;
	final String pwyTitle;
	final String ruleId;
	final String eltId;
	final String ruleDesc;
	final String msg;
	
	public String getGroupingId(boolean isByRule)
	{
		return isByRule ? ruleId : pwyId;
	}
	
	public String getMessage (boolean isByRule)
	{
		return isByRule ? ruleDesc : pwyTitle;
	}
}