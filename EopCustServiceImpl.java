package com.ctg.eop.cust.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ctg.cbs.common.busi.ContactsInfoDto;
import com.ctg.cbs.common.busi.CustAttrDto;
import com.ctg.cbs.common.busi.CustomerDto;
import com.ctg.cbs.common.busi.PartyCertDto;
import com.ctg.cbs.common.busi.cfguse.AttrSpecDto;
import com.ctg.cbs.common.busi.inst.ProdInstAttrDto;
import com.ctg.cbs.common.busi.inst.ProdInstDto;
import com.ctg.cbs.common.busi.staff.CommonRegionDto;
import com.ctg.cbs.common.dto.PageInfoDto;
import com.ctg.cbs.common.dto.QryCondDto;
import com.ctg.cbs.common.dto.RspBaseDto;
import com.ctg.cbs.common.sys.dto.DecryptDto;
import com.ctg.cbs.common.sys.dto.EncryptDto;
import com.ctg.cbs.inst.api.dto.ProdInstBaseDto;
import com.ctg.cbs.inst.api.dto.ProdInstDetailDto;
import com.ctg.cbs.inst.api.dto.QueryProdInstBaseDto;
import com.ctg.cbs.inst.api.facade.IProdInstFacade;
import com.ctg.cbs.inst.api.facade.IProdInstQueryFacade;
import com.ctg.cbs.order.api.facade.ICustomerOrderBusiFacade;
import com.ctg.cbs.party.api.dto.CertInfoDto;
import com.ctg.cbs.party.api.dto.ContactsInfoDtlDto;
import com.ctg.cbs.party.api.dto.CustDetailDto;
import com.ctg.cbs.party.api.dto.PartyDetailDto;
import com.ctg.cbs.party.api.dto.QryCustDetailDto;
import com.ctg.cbs.party.api.dto.UpdateCertInfoDto;
import com.ctg.cbs.party.api.facade.ICommonPartyFacade;
import com.ctg.cbs.party.api.facade.ICustomerFacade;
import com.ctg.eop.core.constant.EOPConstants;
import com.ctg.eop.core.util.XMLParseUtil;
import com.ctg.eop.cust.common.dto.core.EopAttrQryDto;
import com.ctg.eop.cust.common.dto.cust.EopCustContactQryDto;
import com.ctg.eop.cust.common.dto.cust.EopCustDetailQryDto;
import com.ctg.eop.cust.common.exception.EOPCustException;
import com.ctg.eop.cust.service.IEopAcctService;
import com.ctg.eop.cust.service.IEopCustExtService;
import com.ctg.eop.cust.service.IEopCustService;
import com.ctg.eop.cust.service.IEopProdInstService;
import com.ctg.itrdc.paspsdk.common.utils.type.StringUtils;
import com.ctg.m2m.common.constants.M2MConstants;
import com.ctg.m2m.common.constants.M2MExcpMsgConstants;
import com.ctg.m2m.common.constants.M2MExcpMsgConstants.CBS_EXCP_PUB;
import com.ctg.m2m.common.service.impl.AppBaseServiceImpl;
import com.ctg.m2m.common.util.M2MBeanUtils;
import com.ctg.m2m.common.util.M2MConvertUtil;
import com.ctg.m2m.common.util.M2MExceptionUtils;
import com.ctg.m2m.custmgr.api.dto.account.AppAccountQryDto;
import com.ctg.m2m.custmgr.api.dto.account.AppCustomerOrderDto;
import com.ctg.m2m.custmgr.api.dto.custdetail.AppCertInfoDto;
import com.ctg.m2m.custmgr.api.dto.custdetail.AppCustAttrDto;
import com.ctg.m2m.custmgr.api.dto.custdetail.AppCustBusinessInfoDto;
import com.ctg.m2m.custmgr.api.dto.custdetail.AppCustDetailDto;
import com.ctg.m2m.custmgr.api.dto.custdetail.AppCustSimpleDto;
import com.ctg.m2m.custmgr.api.dto.custdetail.AppCustomerDto;
import com.ctg.m2m.custmgr.api.dto.custdetail.AppPartyCertDto;
import com.ctg.m2m.custmgr.api.dto.custdetail.AppPartyDetailDto;
import com.ctg.m2m.custmgr.api.dto.prod.AppProdInstQryDto;
import com.ctg.m2m.custmgr.api.dto.prod.AppProdInstViewDto;
import com.ctg.m2m.orderacpt.api.facade.IAppPubQueryFacade;

/**
 * 
 * @ClassName: EopCustServiceImpl
 * @Description: 客户信息查询等服务处理实现
 * @author liweihui 2016年5月6日 下午4:20:56
 *
 */
@Component("eopCustService")
public class EopCustServiceImpl extends AppBaseServiceImpl implements IEopCustService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EopCustServiceImpl.class);
	protected static final Long ID_DEFAULT_VALUE = -1L;
	protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
	protected static final String MAX_DATE_STR = "20371231000000";

	@Resource
	private ICustomerFacade customerFacade;
	@Resource
	private IProdInstQueryFacade prodInstQueryFacade;
	@Resource
	private IProdInstFacade prodInstFacade;
	@Resource
	private ICustomerOrderBusiFacade customerOrderBusiFacade;
	@Resource
	private ICommonPartyFacade commonPartyFacade;
	@Resource
	private IEopCustExtService appCustExtService;
	@Resource
	private IEopAcctService eopAcctService;
	@Resource
	private IAppPubQueryFacade appPubQueryFacade;
	@Resource
	private IEopProdInstService eopProdInstService;
	@Resource
	private IEopCustService eopCustService;
	
	

	@Override
	public EopCustDetailQryDto qryCustDetail(Map<String, Object> svcMap) throws Exception
	{
		// 查询条件判空
		if (svcMap == null)
		{
			throw new EOPCustException("1999", "入参为空0");
		}
		// 客户编码
		String custNumber = XMLParseUtil.getStringFromMap(svcMap, "custNumber");
		// 产品接入号码
		String phoneNum = XMLParseUtil.getStringFromMap(svcMap, "phoneNum");
		// 证件类型
		String certType = XMLParseUtil.getStringFromMap(svcMap, "certType");
		// 证件号码
		String certNumber = XMLParseUtil.getStringFromMap(svcMap, "certNumber");
		
		String qryType = null;
		String qryValue1 = null;
		String qryValue2 = null;
		if (StringUtils.isNotBlank(custNumber))
		{
			qryType = M2MConstants.QRY_TYPE.CUST_NUMBER;
			qryValue1 = (String) custNumber;
		} else if (StringUtils.isNotBlank(phoneNum))
		{
			qryType = M2MConstants.QRY_TYPE.ACC_NUM;
			qryValue1 = (String) phoneNum;
		} else if (StringUtils.isNotBlank(certNumber))
		{
			if (StringUtils.isEmpty(certType))
			{
				certType = "1";
			}
			qryType = M2MConstants.QRY_TYPE.CERT_TYPT_NUM;
			qryValue1 = (String) certType;
			qryValue2 = (String) certNumber;
		} else
		{
			throw new EOPCustException("1999", "入参为空1");
		}
		
		List<String> queryScopes = new ArrayList<String>();
		// 客户基本信息
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CUSTOMER);
		// 客户证件信息
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.PARTY_CERT); 
		// 联系信息
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CONTACT_INFO);
		// 客户联系信息
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CUST_CONTACT_INFO_REL);
		// 客户属性信息
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CUST_ATTR);

		// 查询类型为接入号码,根据接入号码查询客户的标识
		if (M2MConstants.QRY_TYPE.ACC_NUM.equals(qryType)) {
			List<AppCustSimpleDto> custSimpleDtos = this.qryCustSimpleInfoByAccNum(qryValue1);
			if (CollectionUtils.isEmpty(custSimpleDtos)) {
				LOGGER.error("根据" + qryType + "," + qryValue1 + "未查询到客户信息");
				return null;
			}
			Long cust_id = custSimpleDtos.get(0).getOwnerCustId();
			Long party_id = custSimpleDtos.get(0).getPartyId();
			qryType = M2MConstants.QRY_TYPE.CUST_ID;     // 修改查询类型为cust_id,并修改查询条件值
			qryValue1 = cust_id.toString();
			qryValue2 = party_id.toString();
		}
		
		QryCustDetailDto qryCustDetailDto = new QryCustDetailDto();
		qryCustDetailDto.setApiCode("200200040001".toString());
		QryCondDto queryCondition = new QryCondDto();
		qryValue1 = qryValue1.trim();
		qryValue2 = (null == qryValue2 ? null : qryValue2.trim());
		queryCondition.setQryType(qryType);
		queryCondition.setQryValue1(qryValue1);
		queryCondition.setQryValue2(qryValue2);
		queryCondition.setOwnObjType(M2MConstants.OWN_OBJ_TYPE.PARTY);
		if (!StringUtils.isNullOrEmpty(qryValue2)) {
			queryCondition.setOwnObjId(Long.valueOf(qryValue2));			
		}
		PageInfoDto pageInfo = new PageInfoDto();
		pageInfo.setPageIndex(1);
		pageInfo.setPageSize(1);
		pageInfo.setUseScope(null);
		qryCustDetailDto.setPageInfo(pageInfo);
		qryCustDetailDto.setQueryCondition(queryCondition);
		qryCustDetailDto.setQueryScope(queryScopes);
		
		RspBaseDto rspBaseDto = customerFacade.qryCustDetail(qryCustDetailDto);
		if (null == rspBaseDto)
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(CBS_EXCP_PUB.EXCP_13, "数据中心返回结果为空");
		} 
		if(!M2MConstants.RSP_RESULT_TYPE.SUC.equals(rspBaseDto.getRspResultType())){
			if(StringUtils.isBlank(rspBaseDto.getRspResultDesc())){
				rspBaseDto.setRspResultDesc("根据custNumber查询客户信息为空");
			}
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(CBS_EXCP_PUB.EXCP_13, rspBaseDto.getRspResultDesc());
		}

		CustDetailDto srcCustDetailDto = (CustDetailDto) rspBaseDto.getRspResult();
		if (null == srcCustDetailDto) {
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(CBS_EXCP_PUB.EXCP_16, rspBaseDto.getRspResultDesc());
		}
		
		// EOP接口格式的查询结果
		EopCustDetailQryDto eopCustDetailQryDto = new EopCustDetailQryDto();
		// EOP接口查询结果属性信息
		List<EopAttrQryDto> eopCustAttrs = new ArrayList<EopAttrQryDto>();
		// EOP接口查询结果联系人信息 
		List<EopCustContactQryDto> eopContacts = new ArrayList<EopCustContactQryDto>();

		// 基本信息
		CustomerDto customerDto = srcCustDetailDto.getCustomer();

		if (customerDto != null)
		{
			eopCustDetailQryDto.setCustNumber(customerDto.getCustNumber());
			eopCustDetailQryDto.setCustName(customerDto.getCustName());
			eopCustDetailQryDto.setCustType(customerDto.getCustType());
			eopCustDetailQryDto.setMailAddress(customerDto.getCustAddr());
			Long commonRegionId = customerDto.getRegionId();
			eopCustDetailQryDto.setCommonRegionId(commonRegionId);
			CommonRegionDto commonRegionDto = null;
			try
			{
				commonRegionDto = appPubQueryFacade.qryCommonRegion(commonRegionId);
				if (null != commonRegionDto)
				{
					String commonRegionName = commonRegionDto.getRegionName();
					eopCustDetailQryDto.setCommonRegionName(commonRegionName);
				}
			} catch (Exception e)
			{
				LOGGER.error("程序错误：" , e);
			}
		}
		// 证件信息
		PartyDetailDto partyDetail = srcCustDetailDto.getPartyDetail();
		if (null != partyDetail)
		{
			List<PartyCertDto> partyCerts = partyDetail.getPartyCerts();
			if (partyCerts != null && partyCerts.size() > 0)
			{
				PartyCertDto partyCertDto = partyCerts.get(0);
				eopCustDetailQryDto.setCertType(partyCertDto.getCertType());
				// 证件类型名称
				try
				{
					String certTypeName = appPubQueryFacade.qryAttrValueName("PTY-0004", partyCertDto.getCertType());
					LOGGER.info("【翻译证件类型名称】 certType:" + partyCertDto.getCertType()+",certTypeName" + certTypeName);
					eopCustDetailQryDto.setCertTypeName(certTypeName);
				} catch (Exception e)
				{
					LOGGER.error("程序错误：" , e);
				}
				eopCustDetailQryDto.setCertNumber(partyCertDto.getCertNum());
				eopCustDetailQryDto.setCertAddress(partyCertDto.getCertAddr());
			}
		}

//		eopCustDetailQryDto.setPhoneNum("");
//		eopCustDetailQryDto.setPostCode("");
		

		// 联系人信息 
//		AppPartyDetailDto partyDetail = custDetailDto.getPartyDetail();
		List<ContactsInfoDtlDto> contactsInfoDtls = partyDetail.getContactsInfoDtls();
		if (CollectionUtils.isNotEmpty(contactsInfoDtls))
		{
			for (ContactsInfoDtlDto appContactsInfoDtlDto : contactsInfoDtls)
			{
				ContactsInfoDto appContactsInfoDto = appContactsInfoDtlDto.getContactsInfo();
				EopCustContactQryDto appEOPCustContactQryDto = new EopCustContactQryDto();
				appEOPCustContactQryDto.setContactName(appContactsInfoDto.getContactName());
				appEOPCustContactQryDto.setContactPhoneNum(appContactsInfoDto.getMobilePhone());
				eopCustDetailQryDto.setPhoneNum(appContactsInfoDto.getMobilePhone());
				// 待定（？）
				appEOPCustContactQryDto.setHeadFlag(appContactsInfoDto.getHeadFlag());
				eopContacts.add(appEOPCustContactQryDto);
			}				
		}
		eopCustDetailQryDto.setContactInfos(eopContacts);
		
		// 属性信息 
		List<CustAttrDto> custAttrs = srcCustDetailDto.getCustAttrs();
		if (CollectionUtils.isNotEmpty(custAttrs))
		{
			for (CustAttrDto custAttrDto : custAttrs)
			{
				EopAttrQryDto eopCustAttrQryDto = new EopAttrQryDto();
				eopCustAttrQryDto.setAttrValue(custAttrDto.getAttrValue());
				Long attrID= custAttrDto.getAttrId();
				eopCustAttrQryDto.setAttrSpecId(attrID);
				if (attrID == 20170001L)
				{
					eopCustDetailQryDto.setPostCode(custAttrDto.getAttrValue());
				}
				try
				{
					List<Long> attrIds = new ArrayList<Long>();
					attrIds.add(custAttrDto.getAttrId());
					List<AttrSpecDto> attrs = appPubQueryFacade.qryAttrSpecByIds(attrIds);

					if(CollectionUtils.isNotEmpty(attrs))
					{
						String attrName = attrs.get(0).getAttrName();
						eopCustAttrQryDto.setAttrName(attrName);
					}

				} catch (Exception e)
				{
					LOGGER.error("程序错误：" , e);
				}
				eopCustAttrs.add(eopCustAttrQryDto);
			}
		}
		eopCustDetailQryDto.setAttrInfos(eopCustAttrs);
		
		
		
	
		return eopCustDetailQryDto;
	}

	/**
	 * 查询客户详情信息
	 */
	@SuppressWarnings("unchecked")
	public AppCustDetailDto qryCustDetail(String qryType, String qryValue1, String qryValue2, List<String> queryScopes,
			int pageIndex, int pageSize, String pageUseScope) {
		if (StringUtils.isNullOrEmpty(qryType) || StringUtils.isNullOrEmpty(qryValue1)) {
			LOGGER.error("查询客户详情失败：所需入参为空");
			return new AppCustDetailDto();
		}
		if (StringUtils.isNullOrEmpty(qryValue2) 
				&& (M2MConstants.QRY_TYPE.CUST_ID.equals(qryType) || M2MConstants.QRY_TYPE.CERT_TYPT_NUM.equals(qryType))) {
			LOGGER.error("查询客户详情失败：所需入参为空");
			return new AppCustDetailDto();
		}

		// 默认查询客户基本信息
		if (CollectionUtils.isEmpty(queryScopes)) {
			if (queryScopes == null) {
				queryScopes = new ArrayList<String>();
			}
			queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CUSTOMER); // 客户基本信息
		}
		// 指定范围查询时，默认带上查询客户基本信息
		if (!queryScopes.contains(M2MConstants.PARTY_QRY_SCOPE.CUSTOMER)) {
			queryScopes.add(0, M2MConstants.PARTY_QRY_SCOPE.CUSTOMER);
		}
		
		// 查询类型为接入号码,根据接入号码查询客户的标识
		if (M2MConstants.QRY_TYPE.ACC_NUM.equals(qryType)) {
			List<AppCustSimpleDto> custSimpleDtos = this.qryCustSimpleInfoByAccNum(qryValue1);
			if (CollectionUtils.isEmpty(custSimpleDtos)) {
				LOGGER.error("根据" + qryType + "," + qryValue1 + "未查询到客户信息");
				return new AppCustDetailDto();
			}
			Long cust_id = custSimpleDtos.get(0).getOwnerCustId();
			Long party_id = custSimpleDtos.get(0).getPartyId();
			qryType = M2MConstants.QRY_TYPE.CUST_ID;     // 修改查询类型为cust_id,并修改查询条件值
			qryValue1 = cust_id.toString();
			qryValue2 = party_id.toString();
		}
		
		QryCustDetailDto qryCustDetailDto = new QryCustDetailDto();
		qryCustDetailDto.setApiCode("200200040001".toString());
		QryCondDto queryCondition = new QryCondDto();
		qryValue1 = qryValue1.trim();
		qryValue2 = (null == qryValue2 ? null : qryValue2.trim());
		queryCondition.setQryType(qryType);
		queryCondition.setQryValue1(qryValue1);
		queryCondition.setQryValue2(qryValue2);
		queryCondition.setOwnObjType(M2MConstants.OWN_OBJ_TYPE.PARTY);
		if (!StringUtils.isNullOrEmpty(qryValue2)) {
			queryCondition.setOwnObjId(Long.valueOf(qryValue2));			
		}
		PageInfoDto pageInfo = new PageInfoDto();
		pageInfo.setPageIndex(pageIndex);
		pageInfo.setPageSize(pageSize);
		pageInfo.setUseScope(pageUseScope);
		qryCustDetailDto.setPageInfo(pageInfo);
		qryCustDetailDto.setQueryCondition(queryCondition);
		qryCustDetailDto.setQueryScope(queryScopes);

		try {
			RspBaseDto rspBaseDto = customerFacade.qryCustDetail(qryCustDetailDto);
			if (null == rspBaseDto) {
				LOGGER.error("调用数据服务【20020004】查询客户详情信息返回为null");
				return new AppCustDetailDto();
			}
			if (!M2MConstants.RSP_RESULT_TYPE.SUC.equals(rspBaseDto.getRspResultType())) {
				LOGGER.error("调用数据服务【20020004】查询客户详情信息返回失败，失败原因：" + rspBaseDto.getRspResultCode()
						+ rspBaseDto.getRspResultDesc());
				return new AppCustDetailDto();
			}

			com.ctg.cbs.party.api.dto.CustDetailDto srcCustDetailDto = (com.ctg.cbs.party.api.dto.CustDetailDto) rspBaseDto
					.getRspResult();
			if (null == srcCustDetailDto) {
				LOGGER.error("调用数据服务【20020004】查询客户详情信息返回为空");
				return new AppCustDetailDto();
			}

			AppCustDetailDto destCustDetailDto = new AppCustDetailDto();
			// M2MBeanUtils.copy(destCustDetailDto, srcCustDetailDto);
			destCustDetailDto = M2MConvertUtil.convert(srcCustDetailDto, AppCustDetailDto.class);
			// this.covertCustDetailDto(destCustDetailDto, srcCustDetailDto);
			this.transNameProcess(destCustDetailDto);
			
			// 查询客户业务信息
			if (queryScopes.contains(M2MConstants.PARTY_QRY_SCOPE.BUSINESS_INFO)) {
				AppCustBusinessInfoDto custBusinessInfo = new AppCustBusinessInfoDto();
				
				// 查询产品实例信息
				AppProdInstQryDto appProdInstQryDto = new AppProdInstQryDto();
				appProdInstQryDto.setQryType(M2MConstants.QRY_TYPE.CUST_ID);
				appProdInstQryDto.setQryValue1(destCustDetailDto.getCustomer().getCustId().toString());
				appProdInstQryDto.setPageIndex(pageIndex);
				appProdInstQryDto.setPageSize(pageSize);
				List<AppProdInstViewDto> prodInstViewDtos = (List<AppProdInstViewDto>) appCustExtService.qryProdInstList(appProdInstQryDto).getResult();
				custBusinessInfo.setProdInstViews(prodInstViewDtos);
				
				// 查询客户订单信息
				AppAccountQryDto appAccountQryDto = new AppAccountQryDto();
				appAccountQryDto.setsCond(M2MConstants.QRY_TYPE.CUST_ID);
				appAccountQryDto.setsTxt(destCustDetailDto.getCustomer().getCustId().toString());
				appAccountQryDto.setsVal(destCustDetailDto.getCustomer().getCustId().toString());
				appAccountQryDto.setPageIndex(pageIndex);
				appAccountQryDto.setPageSize(pageSize);
				List<AppCustomerOrderDto> customerOrders = eopAcctService.qryCustomerOrderList(appAccountQryDto);
				custBusinessInfo.setCustomerOrders(customerOrders);
				
				destCustDetailDto.setCustBusinessInfo(custBusinessInfo);
			}
			
			return destCustDetailDto;
		} catch (Exception e) {
			LOGGER.error("调用数据服务【20020004】查询客户详情信息出现异常，异常信息：", e);			
			return new AppCustDetailDto();
		}
	}
	
	// 翻译处理
	private void transNameProcess(AppCustDetailDto custDetailDto) {
		// 客户基本信息
		AppCustomerDto customer = custDetailDto.getCustomer();
		if (customer != null) {
			String custTypeName = "";
			try
			{
				custTypeName = appPubQueryFacade.qryAttrValueName("CUS-0001", customer.getCustType());
			} catch (Exception e)
			{
				e.printStackTrace();
			}

//			String custTypeName = AttrValueCache.getAttrValueName("CUS-0001", customer.getCustType()); // 客户战略分群
			customer.setCustTypeName(custTypeName);
			
			
			String custAreaGradeName = "";
			try
			{
				custAreaGradeName = appPubQueryFacade.qryAttrValueName("CUS-0004", customer.getCustAreaGrade());
			} catch (Exception e)
			{
				e.printStackTrace();
			}
//			String custAreaGradeName = AttrValueCache.getAttrValueName("CUS-0004", customer.getCustAreaGrade());// 客户级别
			customer.setCustAreaGradeName(custAreaGradeName);
			
			String custControlLevel = "";
			try
			{
				custControlLevel = appPubQueryFacade.qryAttrValueName("CUS-C-0017", customer.getCustControlLevel());
			} catch (Exception e)
			{
				e.printStackTrace();
			}
//			String custControlLevel = AttrValueCache.getAttrValueName("CUS-C-0017", customer.getCustControlLevel()); // 客户管控级别
			customer.setCustControlLevel(custControlLevel);
			
			custDetailDto.setCustomer(customer);
		}
		// 证件信息
		AppPartyDetailDto partyDetail = custDetailDto.getPartyDetail();
		if (partyDetail != null) {
			List<AppPartyCertDto> partyCerts = partyDetail.getPartyCerts();
			if (com.ctg.itrdc.paspsdk.common.utils.type.CollectionUtils.isNotEmpty(partyCerts)) {
				for (int p = 0; p < partyCerts.size(); p++) {
					AppPartyCertDto partyCert = partyCerts.get(0);
					if (partyCert != null) {
						String certTypeName = "";
						try
						{
							certTypeName = appPubQueryFacade.qryAttrValueName("PTY-0004", partyCert.getCertType());
						} catch (Exception e)
						{
							e.printStackTrace();
						}
//						String certTypeName = AttrValueCache.getAttrValueName("PTY-0004", partyCert.getCertType());
						partyCert.setCertTypeName(certTypeName);
					}
				}
				partyDetail.setPartyCerts(partyCerts);
			}
			custDetailDto.setPartyDetail(partyDetail);
		}
	}




	/**
	 * 
	 * @Title: qryCustSimpleInfoByAccNum
	 * @Description: 根据接入号查询客户简单信息
	 * @author liweihui 2016年5月7日 下午6:44:33
	 *
	 * @param accNum
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<AppCustSimpleDto> qryCustSimpleInfoByAccNum(String accNum) {
		QueryProdInstBaseDto queryProdInstBaseDto = new QueryProdInstBaseDto();
		QryCondDto qryCondDto = new QryCondDto();
		qryCondDto.setQryType(M2MConstants.QRY_TYPE.ACC_NUM);
		qryCondDto.setQryValue1(accNum);
		qryCondDto.setOwnObjType(M2MConstants.OWN_OBJ_TYPE.CUST); // 分片键类型，可不传
		qryCondDto.setOwnObjId(0l); // TODO 分片键2
		PageInfoDto pageInfo = new PageInfoDto();
		pageInfo.setPageIndex(1);
		pageInfo.setPageSize(10);
		queryProdInstBaseDto.setPageInfo(pageInfo);
		queryProdInstBaseDto.setQueryCondition(qryCondDto);
		queryProdInstBaseDto.setStatusCds(null);// 实例状态
		queryProdInstBaseDto.setProdUseTypes(null);// 用户类型
		queryProdInstBaseDto.setProdId(null); // 实例对应的产品定义标识
		try {
			RspBaseDto rspBaseDto = prodInstQueryFacade.queryProdInstBaseInfo(queryProdInstBaseDto);

			if (null == rspBaseDto) {
				LOGGER.error("调用数据服务【30040001】查询产品实例基本信息返回结果为null");
				return new ArrayList<AppCustSimpleDto>();
			}
			if (!M2MConstants.RSP_RESULT_TYPE.SUC.equals(rspBaseDto.getRspResultType())) {
				LOGGER.error("调用数据服务【30040001】查询产品实例基本信息返回失败，失败原因：" + rspBaseDto.getRspResultCode()
						+ rspBaseDto.getRspResultDesc());
				return new ArrayList<AppCustSimpleDto>();
			}
			List<com.ctg.cbs.inst.api.dto.ProdInstBaseDto> prodInstBaseDtos = (List<com.ctg.cbs.inst.api.dto.ProdInstBaseDto>) rspBaseDto
					.getRspResult();

			if (CollectionUtils.isEmpty(prodInstBaseDtos)) {
				LOGGER.error("调用数据服务【30040001】查询产品实例基本信息返回结果为空");
				return new ArrayList<AppCustSimpleDto>();
			}
			
			Map<Long, AppCustSimpleDto> map = new HashMap<>();			
			for (com.ctg.cbs.inst.api.dto.ProdInstBaseDto prodInstBaseDto : prodInstBaseDtos) {				
				if (null != prodInstBaseDto && null != prodInstBaseDto.getProdInst()) {								
					Long ownerCustId = prodInstBaseDto.getProdInst().getOwnerCustId();
					Long ownerPartyId = prodInstBaseDto.getProdInst().getOwnerPartyId();
					AppCustSimpleDto custSimpleDto = new AppCustSimpleDto();
					custSimpleDto.setOwnerCustId(ownerCustId);
					custSimpleDto.setPartyId(ownerPartyId); // TODO----待模型扩展
					map.put(ownerCustId, custSimpleDto);					
				}
			}
			List<AppCustSimpleDto>  custSimpleDtos = new ArrayList<AppCustSimpleDto>(map.values());
			return custSimpleDtos;
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("调用数据服务【30040001】查询产品实例基本信息出现异常，异常原因：", e);
			return new ArrayList<AppCustSimpleDto>();
		}
	}

	/**
	 * 用户鉴权
	 *
	 * @param svcMap
	 * @return
	 * @throws Exception
	 *
	 */
	@Override
	public Boolean userAuthDetail(Map<String, Object> svcMap) throws Exception
	{
		
		if (svcMap == null)
		{
			throw new EOPCustException("1999", "鉴权失败，鉴权信息为空");		
		}
		// 鉴权类型和接入号
		String authType = XMLParseUtil.getStringFromMap(svcMap, "authType");
		if (StringUtils.isEmpty(authType))
		{
			throw new EOPCustException("1999", "鉴权失败，鉴权类型为空");				
		}
		String phoneNumber  = XMLParseUtil.getStringFromMap(svcMap, "phoneNumber");

		if (StringUtils.isEmpty(phoneNumber))
		{
			throw new EOPCustException("1999", "鉴权失败，接入号为空");		
		}
		
		String password  = XMLParseUtil.getStringFromMap(svcMap, "custPsw");

//		if (StringUtils.isEmpty(password))
//		{
//			throw new EOPCustException("1999", "鉴权失败，密码为空");			
//		}
		
		// 个人鉴权
		if (authType.equals("1"))
		{
			// 使用人证件信息
			Map<String, String> useCertInfoMap = XMLParseUtil.getMapFromMap(svcMap, "useCertInfo");
			if (useCertInfoMap == null)
			{
				throw new EOPCustException("1999", "鉴权失败，使用人证件信息为空");	
			}
			
			Boolean isSucess = this.personalUserAuth( phoneNumber, useCertInfoMap, password);
			return isSucess;
			
		// 企业鉴权
		}else if (authType.equals("2"))
		{
			// 经办人证件信息
			Map<String, String> managerCertInfoMap = XMLParseUtil.getMapFromMap(svcMap, "managerCertInfo");
			// 产权客户认证信息
			Map<String, String> certInfoMap = XMLParseUtil.getMapFromMap(svcMap, "certInfo");
			
			if ((managerCertInfoMap == null))
			{
				throw new EOPCustException("1999", "鉴权失败，经办人证件信息为空");	
			}
			
			if (certInfoMap == null)
			{
				throw new EOPCustException("1999", "鉴权失败，产权客户证件信息为空 ");	
			}
			
			Boolean isSucess = this.organizationalUserAuth(phoneNumber, managerCertInfoMap, certInfoMap, password);
			return isSucess;
		}else 
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_03,"鉴权失败，鉴权类型不符合规范,authType:" + authType );
			return false;
		}

		
	}
	
	

	
	/**
	 * 个人用户鉴权
	 *
	 * @param phoneNum
	 * @param useCertInfoMap
	 * @return
	 * @throws Exception
	 *
	 */
	private Boolean personalUserAuth(String  phoneNum, Map<String, String> useCertInfoMap, String password) throws Exception
	{
		//decrypt("AF9CCC74C8C3B73A");
		String certType = useCertInfoMap.get("certType");
		
		String certNumber = useCertInfoMap.get("certNumber");
		
		if (StringUtils.isEmpty(certType))
		{
			LOGGER.info("鉴权失败，使用人证件类型为空 ");
			return false;
		}		
		if (StringUtils.isEmpty(certNumber))
		{
			LOGGER.info("鉴权失败，使用人证件值为空 ");
			return false;
		}
		// 根据接入号查询产品实例信息
		ProdInstBaseDto prodInstBaseDto = qryProdInstBaseByPhoneNum(phoneNum);			
		if (prodInstBaseDto == null || prodInstBaseDto.getProdInst() == null){
			LOGGER.info("鉴权失败，查询产品实例为空, phoneNumber: " + phoneNum);
			return false;		
		}
		ProdInstDto prodInstDto = prodInstBaseDto.getProdInst();		
		
		// 使用人信息
		Long useCustId = prodInstDto.getUseCustId();
		Long usePartyId = prodInstDto.getUsePartyId();
		
		if (!M2MBeanUtils.isNotNull(useCustId))
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，使用人custId为空");
		}
		
		if (!M2MBeanUtils.isNotNull(usePartyId))
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，使用人partyId为空");
		}
		
		String qryValue1 = Long.toString(useCustId);
		String qryValue2 = Long.toString(usePartyId);
		String qryType = M2MConstants.QRY_TYPE.CUST_ID;
		List<String> queryScopes = new ArrayList<String>();
		// 客户基本信息
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CUSTOMER);
		// 客户证件信息
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.PARTY_CERT);
		// 认证信息
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CERT_INFO);
		
		AppCustDetailDto custDetailDto =  eopCustService.qryCustDetail(qryType, qryValue1, qryValue2, queryScopes, 1, 10, null);

		if (custDetailDto == null)
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，通过使用人custId查询客户信息为空");
		}
		
		AppPartyDetailDto partyDetail = custDetailDto.getPartyDetail();
		if (partyDetail == null )
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，通过使用人custId查询partyDetail为空");
		}
		
		List<AppPartyCertDto> partyCerts = partyDetail.getPartyCerts();
		
		if (CollectionUtils.isEmpty(partyCerts))
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，通过使用人custId查询证件信息为空");
		}
		
		// 验证证件信息
		for(AppPartyCertDto partyCertDto : partyCerts){
			if (certType.equals(partyCertDto.getCertType())){
				if (certNumber.equals(partyCertDto.getCertNum())) {
					LOGGER.info("phoneNum:"+phoneNum+"，证件鉴权成功");
					if(StringUtils.isBlank(password)){   // 若入参未传密码，则不校验密码，直接返回校验成功
						return true;
					}
					
					// 若传了密码，则要校验密码的正确性
					return judgePwdCorrect(custDetailDto, prodInstBaseDto, password);
				}else{
					LOGGER.info("鉴权失败， 使用人证件号码不符, trueCertNumber: " + partyCertDto.getCertNum() + ",certNumber:" + certNumber);
					return false;
				}
			}

		}
		LOGGER.info("鉴权失败，未查询到相应证件信息");
		return false;
	}
	
	/**
	 * 校验密码正确性：若入参传了密码，则需要校验密码正确性（先校验客户密码，不通过再校验产品实例密码）
	 * @return
	 */
	private boolean judgePwdCorrect(AppCustDetailDto custDetailDto, ProdInstBaseDto prodInstBaseDto, String password){
		// 获取‘产品实例密码’
		String prodInstPwd = getProdInstPwd(prodInstBaseDto);  
		
		// 获取‘客户密码’
		String custPwd = null;
		List<AppCertInfoDto> certInfoList = custDetailDto.getCertInfos();
		if(CollectionUtils.isNotEmpty(certInfoList)){
			AppCertInfoDto certInfo = certInfoList.get(0);
			custPwd = certInfo.getPassword();
		}
			
		// 客户密码为空，则直接校验产品实例密码
		if(StringUtils.isBlank(custPwd)){       
			if(StringUtils.isBlank(prodInstPwd)){
				return true;
			}else{
				return this.checkPassword(prodInstPwd, password);
			}
		}else{
			boolean checkResult = this.checkPassword(custPwd, password);
			if(checkResult == true){
				return true;
			}
			// 再校验产口实例密码
			if(StringUtils.isBlank(prodInstPwd)){
				return false;
			}else{
				return this.checkPassword(prodInstPwd, password);
			}
		}
	}
	
	/**
	 * 政企客户鉴权
	 *
	 * @param phoneNumber
	 * @param managerCertInfoMap
	 * @param certInfoMap
	 * @return
	 * @throws Exception 
	 *
	 */
	private Boolean organizationalUserAuth(String phoneNumber, Map<String, String> managerCertInfoMap, Map<String, String> certInfoMap ,String password) throws Exception
	{
		// 经办人证件
		String managerCertType = managerCertInfoMap.get("certType");		
		String managerCertNumber = managerCertInfoMap.get("certNumber");
		
		// 产权客户证件
		String ownCertType = certInfoMap.get("certType");		
		String ownCertNumber = certInfoMap.get("certNumber");
		
		if (StringUtils.isEmpty(managerCertType))
		{
			LOGGER.info("鉴权失败，经办人证件类型为空 ");
			return false;
		}		
		if (StringUtils.isEmpty(managerCertNumber))
		{
			LOGGER.info("鉴权失败，经办人证件值为空 ");
			return false;
		}
		
		if (StringUtils.isEmpty(ownCertType))
		{
			LOGGER.info("鉴权失败，产权客户证件类型为空 ");
			return false;
		}		
		if (StringUtils.isEmpty(ownCertNumber))
		{
			LOGGER.info("鉴权失败，产权客户证件值为空");
			return false;
		}
		
		
		// 根据接入号查询产品实例信息
		ProdInstBaseDto prodInstBaseDto = qryProdInstBaseByPhoneNum(phoneNumber);
		if (prodInstBaseDto == null || prodInstBaseDto.getProdInst() == null){
			LOGGER.info("鉴权失败，查询产品实例为空, phoneNumber: " + phoneNumber);
			return false;
		}
		ProdInstDto prodInstDto = prodInstBaseDto.getProdInst();
		
		// 查询产权客户信息
		Long ownCustId = prodInstDto.getOwnerCustId();
		Long ownPartId = prodInstDto.getOwnerPartyId();
		
		if (!M2MBeanUtils.isNotNull(ownCustId))
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，产权客户custId为空");
		}
		
		if (!M2MBeanUtils.isNotNull(ownPartId))
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，产权客户partId为空");
		}
		
		String qryValue1 = Long.toString(ownCustId);
		String qryValue2 = Long.toString(ownPartId);
		String qryType = M2MConstants.QRY_TYPE.CUST_ID;
		List<String> queryScopes = new ArrayList<String>();
		// 客户基本信息
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CUSTOMER);
		// 客户证件信息
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.PARTY_CERT); 
		// 客户属性信息 
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CUST_ATTR);
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CERT_INFO);
		
		AppCustDetailDto custDetailDto =  eopCustService.qryCustDetail(qryType, qryValue1, qryValue2, queryScopes, 1, 10, null);

		if (custDetailDto == null)
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，通过使用人custId查询客户信息为空");
		}
		
		
		AppPartyDetailDto partyDetail = custDetailDto.getPartyDetail();
		if (partyDetail == null )
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，通过使用人custId查询partyDetail为空");
		}
		
		List<AppPartyCertDto> partyCerts = partyDetail.getPartyCerts();
		
		if (CollectionUtils.isEmpty(partyCerts))
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，通过使用人custId查询证件信息为空");
		}
		
//		List<AppCertInfoDto> certInfos = custDetailDto.getCertInfos();
//		if (CollectionUtils.isEmpty(certInfos))
//		{
//			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，通过使用人custId查询密码信息为空");
//		}
//		AppCertInfoDto appCertInfoDto = certInfos.get(0);
//		String	trueCustPassword = appCertInfoDto.getPassword();
		
//		boolean isPass = this.checkPassword(trueCustPassword, password);
//		
//		if (isPass == false)
//		{
//			throw new EOPCustException("1999", "鉴权失败，密码认证未通过");	
//		}


		
		boolean flag = false;
		// 验证证件信息
		for(AppPartyCertDto partyCertDto : partyCerts)
		{
			if (ownCertType.equals(partyCertDto.getCertType()))
			{
				flag = true;
				if (ownCertNumber.equals(partyCertDto.getCertNum()))
				{
					LOGGER.info("验证产权客户证件成功， ownCertType:" + ownCertType + "; ownCertNumber: " + ownCertNumber);
					break;
				}else {
					LOGGER.info("验证产权客户证件失败， ownCertType:" + ownCertType + "; ownCertNumber: " + ownCertNumber);
					return false;
				}			
			}
//			LOGGER.info("验证产权客户证件失败， 未查询到相应证件信息，ownCertType:" + ownCertType + "; ownCertNumber: " + ownCertNumber);
//			return false;
		}
		if (flag == false)
		{
			LOGGER.info("验证产权客户证件失败， 未查询到相信证件信息");
			return false;
		}
	
		// 属性信息 
		List<AppCustAttrDto> custAttrs = custDetailDto.getCustAttrs();
		

		if (CollectionUtils.isEmpty(custAttrs))
		{
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.M2M_EXCP_PUB.EXCP_16,"鉴权失败，未查询到客户经办人信息");
		}
		
		String trueManagerCertType = "";
		String trueManagerCertNumber = "";
		for(AppCustAttrDto custAttrDto : custAttrs)
		{
			long attrSpecId = custAttrDto.getAttrId();
			if (attrSpecId == 900001008L)
			{
				trueManagerCertType = custAttrDto.getAttrValue();
			}
			
			if (attrSpecId == 900001007L)
			{
				trueManagerCertNumber = custAttrDto.getAttrValue();
			}
		}
		
		if (StringUtils.isEmpty(trueManagerCertType) || StringUtils.isEmpty(trueManagerCertNumber))
		{
			LOGGER.info("验证经办人证件失败， 未查询到相信证件信息");
			return false;
		}
		
		if (trueManagerCertType.equals(managerCertType))
		{
			if (trueManagerCertNumber.equals(managerCertNumber))
			{
				LOGGER.info("验证经办人证件成功");

				if(StringUtils.isBlank(password)){   // 若入参未传密码，则不校验密码，直接返回校验成功
					return true;
				}
				
				// 若传了密码，则要校验密码的正确性
				return judgePwdCorrect(custDetailDto, prodInstBaseDto, password);
			}
			else 
			{
				LOGGER.info("验证经办人证件失败， 证件号码不符, trueManagerCertNumber: " + trueManagerCertNumber + ",managerCertNumber:" + managerCertNumber);
				return false;
			}
		}else 
		{
			LOGGER.info("验证经办人证件失败， 证件类型不符, trueManagerCertType: " + trueManagerCertType + ",managerCertType:" + managerCertType);
			return false;
		}
	}

	
	/**
	 * 根据接入号查询产品实例
	 *
	 * @param phoneNum
	 * @return
	 * @throws Exception
	 *
	 */
	public ProdInstDto qryProdInstByPhoneNum(String phoneNum) throws Exception{
		ProdInstBaseDto prodInstBase = qryProdInstBaseByPhoneNum(phoneNum);
		if(prodInstBase == null){
			return null;
		}
		return prodInstBase.getProdInst();
	}
	
	/**
	 * 从产品实例属性中获取产品实例密码
	 * @param prodInstBaseDto
	 * @return
	 */
	private String getProdInstPwd(ProdInstBaseDto prodInstBaseDto){
		List<ProdInstAttrDto> prodInstAttrList = prodInstBaseDto.getProdInstAttrs();
		if(CollectionUtils.isEmpty(prodInstAttrList)){
			return null;
		}
		
		for(ProdInstAttrDto attrDto : prodInstAttrList){
			// 判断为‘服务密码’的属性ID是否相等
			if(attrDto.getAttrId().longValue() == EOPConstants.PROD_INST_ATTR_ID.PWD.longValue() ){
				return attrDto.getAttrValue();
			}
		}
		
		return null;
	}

	/**
	 * 根据接入号查询产品实例以及相关属性、关联信息
	 * @param phoneNum
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private ProdInstBaseDto qryProdInstBaseByPhoneNum(String phoneNum) throws Exception{
		QueryProdInstBaseDto queryProdInstBaseDto = new QueryProdInstBaseDto();
		QryCondDto qryCondDto = new QryCondDto();
		qryCondDto.setQryType(M2MConstants.QRY_TYPE.ACC_NUM);
		qryCondDto.setQryValue1(phoneNum);
		qryCondDto.setOwnObjType(M2MConstants.OWN_OBJ_TYPE.CUST);
		queryProdInstBaseDto.setQueryCondition(qryCondDto);

		PageInfoDto pageInfo = new PageInfoDto();
		pageInfo.setPageIndex(1);
		pageInfo.setPageSize(10);
		queryProdInstBaseDto.setPageInfo(pageInfo);

		RspBaseDto rsp = prodInstQueryFacade.eopQueryProdInstBaseInfo(queryProdInstBaseDto);
		
		if (null == rsp) {
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.CBS_EXCP_PUB.EXCP_13,"无法调用数据服务【30040001】!");
		}
		if (!M2MConstants.RSP_RESULT_TYPE.SUC.equals(rsp.getRspResultType())) {
			M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.CBS_EXCP_PUB.EXCP_13,"调用数据服务【30040001】返回失败，原因：" + rsp.getRspResultCode() + rsp.getRspResultDesc());
		}
		if (rsp.getRspResult() == null) {
			LOGGER.error("调用数据服务【30040001】查询在用产品实例列表服务返回值为NULL");
		}
		
		if (rsp != null && M2MConstants.RSP_RESULT_TYPE.SUC.equals(rsp.getRspResultType()) && rsp.getRspResult() != null){
			List<ProdInstBaseDto> result = (List<ProdInstBaseDto>) rsp.getRspResult();
			if (CollectionUtils.isNotEmpty(result)){
				return result.get(0);				
			}
		}
		return null;
	}
	
	/**
	 * 修改密码
	 *
	 * @param svcMap
	 * @return
	 * @throws Exception
	 *
	 */
	@Override
	public Boolean updatePassword(Map<String, Object> svcMap) throws Exception
	{
		
		if (svcMap == null)
		{
			throw new EOPCustException("1999", "修改密码失败，入参为空");
		}

		String phoneNumber  = XMLParseUtil.getStringFromMap(svcMap, "phoneNumber");

		if (StringUtils.isEmpty(phoneNumber))
		{
			throw new EOPCustException("1999", "修改密码失败，接入号为空");
	
		}		
		// 
		String oldPassword = XMLParseUtil.getStringFromMap(svcMap, "oldPassword");
		if (StringUtils.isEmpty(oldPassword))
		{
			throw new EOPCustException("1999", "修改密码失败，原密码为空");		
		}
		
		// 
		String newPassword = XMLParseUtil.getStringFromMap(svcMap, "newPassword");
		if (StringUtils.isEmpty(newPassword))
		{
			throw new EOPCustException("1999", "修改密码失败，新密码为空");	
		}
		
		
		// 根据接入号查询产品实例信息
		ProdInstDto prodInstDto = this.qryProdInstByPhoneNum(phoneNumber);			
		if (prodInstDto == null)
		{
			throw new EOPCustException("1999", "修改密码失败，查询产品实例为空, phoneNumber: " + phoneNumber);		
		}
		
		// 产权客户信息
		Long ownCustId = prodInstDto.getOwnerCustId();
		Long ownPartyId = prodInstDto.getOwnerPartyId();
		
		if (!M2MBeanUtils.isNotNull(ownCustId))
		{
			throw new EOPCustException("1999", "修改密码失败，产权客户custId为空");		
		}
		
		if (!M2MBeanUtils.isNotNull(ownPartyId))
		{
			throw new EOPCustException("1999", "修改密码失败，产权客户partyId为空");		
		}
		
		String qryValue1 = Long.toString(ownCustId);
		String qryValue2 = Long.toString(ownPartyId);
		String qryType = M2MConstants.QRY_TYPE.CUST_ID;
		List<String> queryScopes = new ArrayList<String>();
		// 客户基本信息
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CUSTOMER);
		queryScopes.add(M2MConstants.PARTY_QRY_SCOPE.CERT_INFO);

		
		AppCustDetailDto custDetailDto =  eopCustService.qryCustDetail(qryType, qryValue1, qryValue2, queryScopes, 1, 10, null);

		if (custDetailDto == null)
		{
			throw new EOPCustException("1999", "修改密码失败，通过产权客户custId查询客户信息为空");		
		}
		
		AppCustomerDto customer = custDetailDto.getCustomer();
		if (customer == null)
		{
			throw new EOPCustException("1999", "修改密码失败，通过产权客户custId查询客户信息为空");		
		}
		
		String custType = customer.getCustType();
		// 政企客户
		if ("1000".equals(custType))
		{
			String trueCustPassword = "";
			Long certId = 0L;
			List<AppCertInfoDto> certInfos = custDetailDto.getCertInfos();
			if (CollectionUtils.isNotEmpty(certInfos))
			{
				AppCertInfoDto appCertInfoDto = certInfos.get(0);
				trueCustPassword = appCertInfoDto.getPassword();
				certId = appCertInfoDto.getCertId();
			}else 
			{
				throw new EOPCustException("1999", "修改密码失败，产权客户密码信息为空");		
			}
			
			boolean isPass = this.checkPassword(trueCustPassword, oldPassword);
			if(isPass == false)
			{
				throw new EOPCustException("1999", "修改密码失败，原密码错误");		
			}
			UpdateCertInfoDto updateCertInfoDto = new UpdateCertInfoDto();
			CertInfoDto certInfoDto = new CertInfoDto();
			certInfoDto.setPassword(this.encrypt(newPassword));
			certInfoDto.setCertId(certId);
			updateCertInfoDto.setCertInfo(certInfoDto);
			
			RspBaseDto rsp = customerFacade.eopUpdateCustPassword(updateCertInfoDto);
			if (null == rsp) 
			{
				M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.CBS_EXCP_PUB.EXCP_13,"无法调用更新密码数据服务!");
			}
			if (!M2MConstants.RSP_RESULT_TYPE.SUC.equals(rsp.getRspResultType())) 
			{
				M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.CBS_EXCP_PUB.EXCP_13,"更新密码失败，原因：" + rsp.getRspResultCode() + rsp.getRspResultDesc());
			}
			
			// 更新密码成功
			return true;		
			
		// 个人客户
		}else if ("1100".equals(custType)) 
		{ 
			String trueUserPassword = prodInstDto.getProdInstPwd();
			boolean isPass = this.checkPassword(trueUserPassword, oldPassword);
			if(isPass == false)
			{
				throw new EOPCustException("1999", "修改密码失败，原密码错误");		
			}
			
			ProdInstDetailDto prodInstDetailDto = new ProdInstDetailDto();
			ProdInstDto newProdInstDto = new ProdInstDto();
			newProdInstDto.setProdInstPwd(this.encrypt(newPassword));
			newProdInstDto.setProdInstId(prodInstDto.getProdInstId());
			prodInstDetailDto.setProdInst(newProdInstDto);
			
			RspBaseDto rsp = prodInstFacade.eopUpdateCustPassword(prodInstDetailDto);
			if (null == rsp) 
			{
				M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.CBS_EXCP_PUB.EXCP_13,"无法调用更新密码数据服务!");
			}
			if (!M2MConstants.RSP_RESULT_TYPE.SUC.equals(rsp.getRspResultType())) 
			{
				M2MExceptionUtils.throwApplictionRuntimeExceptionByEm(M2MExcpMsgConstants.CBS_EXCP_PUB.EXCP_13,"更新密码失败，原因：" + rsp.getRspResultCode() + rsp.getRspResultDesc());
			}
			
			// 更新密码成功
			return true;	
			
		}else 
		{
			throw new EOPCustException("1999", "修改密码失败，客户类型支持修改密码:" + custType);		
		}
	}
	
	private boolean checkPassword(String truePassword, String custPsw)
	{

		if (StringUtils.isEmpty(truePassword)||StringUtils.isEmpty(custPsw))
		{
			return false;
		}
			if (custPsw.equals(truePassword))
			{
				return true;
			}else 
			{
				// 加密，并验证密文
				String ciphertext = this.encrypt(custPsw);
				if (ciphertext.equals(truePassword))
				{
					return true;
				}else 
				{
					return false;
				}
				
			}
		
	}
	
	private void decrypt(String pwd){
		DecryptDto decryptDto = new DecryptDto();
		decryptDto.setPassword(pwd);
		decryptDto.setStandard("RC2");
		try {
			RspBaseDto rsp = commonPartyFacade.decrypt(decryptDto);
			System.out.println("密文"+pwd+"，解密后为:"+rsp.getRspResult().toString()) ;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private String encrypt(String password) {

		// 校验传入参数
		if (StringUtils.isNullOrEmpty(password)) {
			LOGGER.error("密码加密失败：所需入参为空");
			return "";
		}

		// 创建数据服务所需要的对象
		EncryptDto encrypt = new EncryptDto();

		try {
			// 
			encrypt.setPassword(password);
			encrypt.setStandard("RC2");
			// 请求服务
			RspBaseDto rsp = commonPartyFacade.encrypt(encrypt);
			// 处理返回结果
			if (null == rsp) {
				LOGGER.error("调用数据服务-密码加密服务,返回为null");
				return "";
			}
			if (!M2MConstants.RSP_RESULT_TYPE.SUC.equals(rsp.getRspResultType())) {
				// 日志记录失败原因,同时仍将错误信息返回前端
				LOGGER.error("调用数据服务-密码加密服务,返回失败，失败原因：" + rsp.getRspResultCode() + rsp.getRspResultDesc());
				return "";
			}
			
			return rsp.getRspResult().toString();

		} catch (Exception e) {
			LOGGER.error("调用数据服务-密码加密服务,出现异常，异常原因：", e);
			return "";
		}
	}
}
