/**
 * 
 */
package com.etekcity.vbmp.common.comm.dao.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

/**
 * @author puyol
 *
 */
@Data
public class FireBaseInfo {

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "SELECT next value for MYCATSEQ_GLOBAL")
	    private Integer id;
	    
	    private String modelName;
	    
	    private String msgKey;
	    
	    private String msgValue;
}
