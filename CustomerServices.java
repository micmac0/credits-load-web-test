package com.cardlinkin.offerap.server.rest.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cardlinkin.offerap.dao.IAccountOperationAcidDao;
import com.cardlinkin.offerap.dao.IAcidDao;
import com.cardlinkin.offerap.model.bean.AccountOperationAcid;
import com.cardlinkin.offerap.model.bean.Acid;
import com.cardlinkin.offerap.server.rest.ICustomerServices;
import com.cardlinkin.offerap.server.rest.bean.customer.AccountOperationBean;
import com.cardlinkin.offerap.server.rest.bean.customer.AccountOperationResponse;
import com.cardlinkin.offerap.server.rest.bean.customer.AccountOperationsByAcidResponse;
import com.cardlinkin.offerap.server.rest.bean.customer.AcidPreferences;
import com.cardlinkin.offerap.server.rest.bean.customer.AcidUpdateResponse;
import com.cardlinkin.offerap.server.rest.bean.customer.CustomerInfo;
import com.cardlinkin.offerap.server.rest.bean.customer.MoneyTransfertAcid;
import com.cardlinkin.offerap.server.rest.bean.customer.MoneyTransfertResponse;
import com.cardlinkin.offerap.server.rest.bean.customer.RequestMoneyTransfertByAcidRequest;
import com.cardlinkin.offerap.server.rest.bean.customer.RequestMoneyTransfertByAcidResponse;
import com.cardlinkin.offerap.server.rest.bean.customer.UpdateCustomerPreferencesByAcidRequest;
import com.cardlinkin.offerap.server.rest.bean.customer.UpdateCustomerPreferencesByAcidResponse;
import com.cardlinkin.offerap.server.rest.bean.util.ErrorEntity;
import com.cardlinkin.offerap.util.OfferAPConstantes;
import com.cardlinkin.offerap.util.RestUtil;
import com.cdlk.recpg.remote.base.beans.Entry;
import com.cdlk.recpg.remote.pub.beans.AccountOperationOut;
import com.cdlk.recpg.remote.pub.beans.CustomerInfosOut;
import com.cdlk.recpg.remote.pub.beans.CustomerInfosUpdate;
import com.cdlk.recpg.remote.pub.beans.MoneyTransferRequest;
import com.cdlk.recpg.remote.pub.beans.MoneyTransferRequest.TargetIban;
import com.cdlk.recpg.remote.pub.soap.PublishingSoap;
import com.cdlk.recpg.remote.pub.transport.GetAccountOperationsByBcidRequest;
import com.cdlk.recpg.remote.pub.transport.GetAccountOperationsByBcidResponse;
import com.cdlk.recpg.remote.pub.transport.GetCustomerInfosRequest;
import com.cdlk.recpg.remote.pub.transport.GetCustomerInfosResponse;
import com.cdlk.recpg.remote.pub.transport.RequestMoneyTransferRequest;
import com.cdlk.recpg.remote.pub.transport.RequestMoneyTransferResponse;
import com.cdlk.recpg.remote.pub.transport.UpdateCustomerInfosRequest;
import com.cdlk.recpg.remote.pub.transport.UpdateCustomerInfosResponse;

public class CustomerServices implements ICustomerServices {
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerServices.class);

	private IAcidDao acidDao;
	private IAccountOperationAcidDao accountOperationAcidDao;
	private Boolean testMode;
	private PublishingSoap publishingSoap;

	@Override
	public Response getCustomerInfoByAcid(String acid) {
		if (testMode) {
			return getCustomerInfoByAcidMock(acid);
		} else {
			return getCustomerInfoByAcidRecpg(acid);
		}

	}

	private Response getCustomerInfoByAcidRecpg(String acid) {
		Response response;
		CustomerInfo customerInfo = new CustomerInfo();
		try {
			GetCustomerInfosRequest request = new GetCustomerInfosRequest();
			request.setSource(OfferAPConstantes.APP_NAME);
			request.setBcid(acid);
			GetCustomerInfosResponse responseSoap = publishingSoap.getCustomerInfos(request);
			customerInfo.setAcid(responseSoap.getBcid());
			CustomerInfosOut infos = responseSoap.getInfos();
			if (infos == null) {
				return Response.status(Status.NOT_FOUND).build();
			}

			if (infos.getTotalAmountAvailable() == null) {
				customerInfo.setTotalAmountAvailable((double) 0);
			} else {
				customerInfo.setTotalAmountAvailable(infos.getTotalAmountAvailable().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			if (infos.getTotalAmountPaid() == null) {
				customerInfo.setTotalAmountPaid((double) 0);
			} else {
				customerInfo.setTotalAmountPaid(infos.getTotalAmountPaid().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			if (infos.getTotalAmountPending() == null) {
				customerInfo.setTotalAmountPending((double) 0);
			} else {
				customerInfo.setTotalAmountPending(infos.getTotalAmountPending().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			if (infos.getTotalAmountTransfer() == null) {
				customerInfo.setTotalAmountTransfer((double) 0);
			} else {
				customerInfo.setTotalAmountTransfer(infos.getTotalAmountTransfer().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			if (infos.getTotalCashbackRefused() == null) {
				customerInfo.setTotalAmountRefused(0d);
			} else {
				customerInfo.setTotalAmountRefused(infos.getTotalCashbackRefused().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			if (infos.getTotalAmountRejected() == null) {
				customerInfo.setTotalAmountRejected(0d);
			} else {
				customerInfo.setTotalAmountRejected(infos.getTotalAmountRejected().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			customerInfo.setBirthDate(infos.getBirthDate());
			customerInfo.setCivility(infos.getTitle());
			customerInfo.setEmail(infos.getEmail());
			customerInfo.setEntity(infos.getEntity());
			customerInfo.setFirstName(infos.getFirstName());
			customerInfo.setLastName(infos.getName());
			if (infos.getNewsLetter() == null || infos.getNewsLetter() == 0) {
				customerInfo.setHasNewsletter(false);
			} else {
				customerInfo.setHasNewsletter(true);
			}
			customerInfo.setSegment(infos.getSegment());
			customerInfo.setZipCode(infos.getZip());
		} catch (Exception e) {
			LOGGER.error("Erreur while getting customer info ", e);
			ErrorEntity errorEntity = new ErrorEntity();
			errorEntity.setCode(OfferAPConstantes.HTTP_SERVER_ERROR);
			errorEntity.setMessage(e.getMessage());
			response = Response.status(OfferAPConstantes.HTTP_SERVER_ERROR).entity(errorEntity).build();
			return response;
		}
		return Response.ok().entity(customerInfo).build();
	}

	private Response getCustomerInfoByAcidMock(String acid) {
		Response response;
		CustomerInfo customerInfo = new CustomerInfo();
		try {
			if (acid == null || acid.isEmpty()) {
				ErrorEntity errorEntity = new ErrorEntity();
				errorEntity.setCode(OfferAPConstantes.HTTP_MISSING_PARAMETERS);
				errorEntity.setMessage("acid required : " + acid);
				response = Response.status(OfferAPConstantes.HTTP_MISSING_PARAMETERS).entity(errorEntity).build();
				return response;
			}
			Acid acidDb = acidDao.getAcid(acid);
			if (acidDb == null) {
				ErrorEntity errorEntity = new ErrorEntity();
				errorEntity.setCode(OfferAPConstantes.HTTP_NOT_FOUND);
				errorEntity.setMessage("acid not found : " + acid);
				response = Response.status(OfferAPConstantes.HTTP_NOT_FOUND).entity(errorEntity).build();
				return response;
			}
			customerInfo.setAcid(acidDb.getAcid());
			customerInfo.setBirthDate(acidDb.getBirthDate());
			customerInfo.setCivility(acidDb.getCivility());
			customerInfo.setEmail(acidDb.getEmail());
			customerInfo.setEntity(acidDb.getEntity());
			customerInfo.setFirstName(acidDb.getFirstName());
			customerInfo.setLastName(acidDb.getLastName());
			if (acidDb.getNewsletter() != null && acidDb.getNewsletter() == 1) {
				customerInfo.setHasNewsletter(true);
			} else {
				customerInfo.setHasNewsletter(false);
			}
			customerInfo.setSegment(acidDb.getSegment());
			customerInfo.setZipCode(acidDb.getZipCode());
			customerInfo.setTotalAmountAvailable(acidDb.getAvailableBalance());
			customerInfo.setTotalAmountPaid(acidDb.getTotalTransfert());
			customerInfo.setTotalAmountTransfer(acidDb.getRunBalanceTransfert());
			customerInfo.setTotalAmountPending(acidDb.getUnavailableBalance());
			// customerInfo.setTotalAmountRefused(acid);
		} catch (Exception e) {
			ErrorEntity errorEntity = new ErrorEntity();
			errorEntity.setCode(OfferAPConstantes.HTTP_SERVER_ERROR);
			String uidError = UUID.randomUUID().toString();
			errorEntity.setMessage("erreur de traitement côté serveur : " + uidError);
			response = Response.status(OfferAPConstantes.HTTP_SERVER_ERROR).entity(errorEntity).build();
			LOGGER.error(uidError + " : erreur lors de la récupération des informations client", e);
			return response;
		}
		return Response.ok().entity(customerInfo).build();
	}

	@Override
	public Response updateCustomerPreferencesByAcid(UpdateCustomerPreferencesByAcidRequest request) {

		if (testMode) {
			return updateCustomerPreferencesByAcidMock(request);
		} else {
			return updateCustomerPreferencesByAcidRecpg(request);
		}

	}

	private Response updateCustomerPreferencesByAcidRecpg(UpdateCustomerPreferencesByAcidRequest request) {
		Response response = null;
		UpdateCustomerPreferencesByAcidResponse entity = new UpdateCustomerPreferencesByAcidResponse();
		try {
			List<AcidUpdateResponse> responseList = new ArrayList<AcidUpdateResponse>();
			UpdateCustomerInfosRequest requestSoap = new UpdateCustomerInfosRequest();
			for (AcidPreferences acidPref : request.getUpdateList()) {
				AcidUpdateResponse acidResp = new AcidUpdateResponse();
				try {
					requestSoap.setSource(OfferAPConstantes.APP_NAME);
					requestSoap.setBcid(acidPref.getAcid());
					CustomerInfosUpdate infos = new CustomerInfosUpdate();
					if (acidPref.getAcid() == null || acidPref.getAcid().isEmpty()) {
						acidResp.setCode(PARAM_ACID_EMPTY);
						acidResp.setAcid(acidPref.getAcid());
					} else {
						if (acidPref.getHasNewsletter() != null && acidPref.getHasNewsletter() == true) {
							infos.setNewsLetter("1");
						} else if (acidPref.getHasNewsletter() != null && acidPref.getHasNewsletter() == false) {
							infos.setNewsLetter("0");
						}
						requestSoap.setInfos(infos);
						UpdateCustomerInfosResponse responseSoap = publishingSoap.updateCustomerInfos(requestSoap);
						if (responseSoap.getGlobalCode() == 0) {
							acidResp.setAcid(acidPref.getAcid());
							acidResp.setCode(OK);
						} else {
							acidResp.setAcid(acidPref.getAcid());
							acidResp.setCode(KO);
						}
					}

				} catch (Exception e) {
					acidResp.setAcid(acidPref.getAcid());
					acidResp.setCode(KO);
				}
				responseList.add(acidResp);
			}
			entity.setResponseList(responseList);
		} catch (Exception e) {
			ErrorEntity errorEntity = new ErrorEntity();
			errorEntity.setCode(OfferAPConstantes.HTTP_SERVER_ERROR);
			String uidError = UUID.randomUUID().toString();
			errorEntity.setMessage("erreur de traitement côté serveur : " + uidError);
			response = Response.status(OfferAPConstantes.HTTP_SERVER_ERROR).entity(errorEntity).build();
			LOGGER.error(uidError + " : erreur lors de la modification des préférences", e);
			return response;
		}
		return Response.ok().entity(entity).build();
	}

	private Response updateCustomerPreferencesByAcidMock(UpdateCustomerPreferencesByAcidRequest request) {
		Response response = null;
		UpdateCustomerPreferencesByAcidResponse entity = new UpdateCustomerPreferencesByAcidResponse();
		try {
			List<AcidUpdateResponse> responseList = new ArrayList<AcidUpdateResponse>();
			for (AcidPreferences acidPref : request.getUpdateList()) {
				AcidUpdateResponse acidUpdateResponse = new AcidUpdateResponse();
				if (acidPref.getAcid() == null || acidPref.getAcid().isEmpty()) {
					acidUpdateResponse.setCode(PARAM_ACID_EMPTY);
					acidUpdateResponse.setAcid(acidPref.getAcid());
				} else {
					Acid acidDb = acidDao.getAcid(acidPref.getAcid());
					if (acidDb == null) {
						acidUpdateResponse.setCode(PARAM_ACID_NOT_FOUND);
						acidUpdateResponse.setAcid(acidPref.getAcid());
					} else {
						if (acidPref.getHasNewsletter() != null && acidPref.getHasNewsletter() == true) {
							acidDb.setNewsletter(1);
						}
						if (acidPref.getHasNewsletter() != null && acidPref.getHasNewsletter() == false) {
							acidDb.setNewsletter(0);
						}
						acidDao.save(acidDb);
						acidUpdateResponse.setCode(OK);
						acidUpdateResponse.setAcid(acidPref.getAcid());
					}
				}
				responseList.add(acidUpdateResponse);
			}
			entity.setResponseList(responseList);
		} catch (Exception e) {
			ErrorEntity errorEntity = new ErrorEntity();
			errorEntity.setCode(OfferAPConstantes.HTTP_SERVER_ERROR);
			String uidError = UUID.randomUUID().toString();
			errorEntity.setMessage("erreur de traitement côté serveur : " + uidError);
			response = Response.status(OfferAPConstantes.HTTP_SERVER_ERROR).entity(errorEntity).build();
			LOGGER.error(uidError + " : erreur lors de la modification des préférences", e);
			return response;
		}
		return Response.ok().entity(entity).build();
	}

	@Override
	public Response getAccountOperationsByAcid(List<String> acids) {
		if (testMode) {
			return getAccountOperationsByAcidMock(acids);
		} else {
			return getAccountOperationsByAcidRecpg(acids);
		}

	}

	private Response getAccountOperationsByAcidRecpg(List<String> acids) {
		SimpleDateFormat sdfRestDateCourt = new SimpleDateFormat(RestUtil.date_format_rest_court);
		SimpleDateFormat sdfRestHourCourt = new SimpleDateFormat(RestUtil.hour_format_rest_court);
		AccountOperationResponse entity = new AccountOperationResponse();
		try {
			List<AccountOperationsByAcidResponse> responseList = new ArrayList<AccountOperationsByAcidResponse>();
			if (acids == null || acids.isEmpty()) {
				ErrorEntity errorEntity = new ErrorEntity();
				errorEntity.setCode(OfferAPConstantes.HTTP_MISSING_PARAMETERS);
				errorEntity.setMessage("missing acids list: ");
				Response response = Response.status(OfferAPConstantes.HTTP_MISSING_PARAMETERS).entity(errorEntity).build();
				return response;
			}
			if (acids.size() == 1 && acids.get(0).contains(",")) {
				String[] tabAcids = acids.get(0).split(",");
				acids.clear();
				acids.addAll(Arrays.asList(tabAcids));
			}
			GetAccountOperationsByBcidRequest requestSoap = new GetAccountOperationsByBcidRequest();

			for (String acid : acids) {
				List<AccountOperationBean> operationList = new ArrayList<AccountOperationBean>();
				AccountOperationsByAcidResponse acidResp = new AccountOperationsByAcidResponse();
				acidResp.setAcid(acid);
				requestSoap.setSource(OfferAPConstantes.APP_NAME);
				requestSoap.setBcid(acid);
				GetAccountOperationsByBcidResponse responseSoap = publishingSoap.getAccountOperationsByBcid(requestSoap);

				if (responseSoap.getOperations() != null && responseSoap.getOperations().getOperation() != null && responseSoap.getOperations().getOperation().size() > 0) {
					for (AccountOperationOut op : responseSoap.getOperations().getOperation()) {
						AccountOperationBean opBean = new AccountOperationBean();
						if (op.getTargetIban() != null && op.getTargetIban().getEntry() != null) {
							StringBuilder sb = new StringBuilder();
							String sep = "";
							for (Entry entry : op.getTargetIban().getEntry()) {
								sb.append(sep).append(entry.getKey()).append("=").append(entry.getValue());
								sep = ";";
							}
							opBean.setAccountDest(sb.toString());
						}
						opBean.setCashBackFlag(op.getFlagCashback());

						if (op.getUpdateTstamp() != null) {
							LOGGER.info("date : " + op.getUpdateTstamp().toGregorianCalendar().getTime());
							Date dateLast = op.getUpdateTstamp().toGregorianCalendar().getTime();
							String strDateLast = sdfRestDateCourt.format(dateLast);
							String strHourLast = sdfRestHourCourt.format(dateLast);
							opBean.setLastChangeDate(strDateLast);
							opBean.setLastChangeHour(strHourLast);
						}
						opBean.setMerid(op.getMerid());
						opBean.setMerchantName(op.getName());
						opBean.setSuperMerid(op.getMmid());
						opBean.setSuperMerchantName(op.getMmName());
						if (op.getOpAmountTi() != null) {
							opBean.setOperationAmount(op.getOpAmountTi().doubleValue());
							if (op.getOpAmountTi().doubleValue() > 0)
								opBean.setCreditDebit(0);
							else
								opBean.setCreditDebit(1);
						}
						if (op.getCbAmountRefused() != null)
							opBean.setAmountRefused(op.getCbAmountRefused().doubleValue());

						opBean.setComment(op.getComment());
						opBean.setCbFlagCause(op.getCause());
						if (op.getCreationTstamp() != null) {
							Date dateOp = op.getCreationTstamp().toGregorianCalendar().getTime();
							String strDateOp = sdfRestDateCourt.format(dateOp);
							String strHourOp = sdfRestHourCourt.format(dateOp);
							opBean.setOperationDate(strDateOp);
							opBean.setOperationHour(strHourOp);
						}
						opBean.setOperationId(op.getOpid());
						opBean.setOrigin(op.getTypeOp());
						opBean.setStatus(op.getStatus());
						if (op.getTrxAmountTi() != null) {
							opBean.setTransactionAmount(op.getTrxAmountTi().doubleValue());
						}
						opBean.setTransactionDate(op.getDateOp());
						opBean.setTransactionHour(op.getTimeOp());

						opBean.setCpgId(op.getCpgId());
						opBean.setCpgName(op.getCpgName());

						opBean.setCbType(op.getCbType());
						if (op.getCbValue() != null)
							opBean.setCbBase(op.getCbValue().doubleValue());
						if (op.getBoostValue() != null)
							opBean.setCbBoost(op.getBoostValue().doubleValue());
						opBean.setPanL4D(op.getPanL4D());

						if (op.getBaseAmount() != null) {
							opBean.setBaseAmount(op.getBaseAmount().doubleValue());
						}
						if (op.getBoostValue() != null) {
							opBean.setBoostValue(op.getBoostValue().doubleValue());
						}
						opBean.setCause(op.getCause());
						opBean.setGoid(op.getGoid());
						opBean.setOfferName(op.getOfferName());
						opBean.setOpId(op.getOpid());

						operationList.add(opBean);
					}
				}
				acidResp.setOperationList(operationList);
				responseList.add(acidResp);
			}
			entity.setResponseList(responseList);
		} catch (Exception e) {
			ErrorEntity errorEntity = new ErrorEntity();
			errorEntity.setCode(OfferAPConstantes.HTTP_SERVER_ERROR);
			String uidError = UUID.randomUUID().toString();
			errorEntity.setMessage("erreur de traitement côté serveur : " + uidError);
			Response response = Response.status(OfferAPConstantes.HTTP_SERVER_ERROR).entity(errorEntity).build();
			LOGGER.error(uidError + " : erreur lors de la récupération des opérations de cagnotte", e);
			return response;
		}
		return Response.ok().entity(entity).build();
	}

	private Response getAccountOperationsByAcidMock(List<String> acids) {
		SimpleDateFormat sdfRestDateCourt = new SimpleDateFormat(RestUtil.date_format_rest_court);
		SimpleDateFormat sdfRestHourCourt = new SimpleDateFormat(RestUtil.hour_format_rest_court);
		AccountOperationResponse entity = new AccountOperationResponse();
		try {
			List<AccountOperationsByAcidResponse> responseList = new ArrayList<AccountOperationsByAcidResponse>();
			if (acids == null || acids.isEmpty()) {
				ErrorEntity errorEntity = new ErrorEntity();
				errorEntity.setCode(OfferAPConstantes.HTTP_MISSING_PARAMETERS);
				errorEntity.setMessage("missing acids list: ");
				Response response = Response.status(OfferAPConstantes.HTTP_MISSING_PARAMETERS).entity(errorEntity).build();
				return response;
			}
			if (acids.size() == 1 && acids.get(0).contains(",")) {
				String[] tabAcids = acids.get(0).split(",");
				acids.clear();
				acids.addAll(Arrays.asList(tabAcids));
			}
			for (String acid : acids) {
				AccountOperationsByAcidResponse acidResp = new AccountOperationsByAcidResponse();
				acidResp.setAcid(acid);
				List<AccountOperationAcid> ops = accountOperationAcidDao.getAcidOperations(acid);
				List<AccountOperationBean> operationList = new ArrayList<AccountOperationBean>();
				for (AccountOperationAcid op : ops) {
					AccountOperationBean opBean = new AccountOperationBean();
					opBean.setAccountDest(op.getDestAccount());
					opBean.setCashBackFlag(op.getCashbackFlag());
					opBean.setCreditDebit(op.getCreditDebit());
					opBean.setLastChangeDate(sdfRestDateCourt.format(op.getLastChangeTimestamp()));
					opBean.setLastChangeHour(sdfRestHourCourt.format(op.getLastChangeTimestamp()));
					opBean.setMerchantName(op.getMerchantName());
					// opBean.setMmid(op.getMmid());
					opBean.setCreditDebit(0);
					// opBean.setMmidType(op.getMmidType());
					opBean.setOperationAmount(op.getAmount());
					opBean.setOperationDate(sdfRestDateCourt.format(op.getCreateTimestamp()));
					opBean.setOperationHour(sdfRestHourCourt.format(op.getCreateTimestamp()));
					opBean.setOperationId(op.getOperationId().toString());
					opBean.setOrigin(op.getOrigin());
					opBean.setStatus(op.getStatus());
					opBean.setTransactionAmount(op.getTransactionAmount());
					opBean.setTransactionDate(String.format("%08d", op.getTransactionDate()));
					opBean.setTransactionHour(String.format("%06d", op.getTransactionHour()));
					operationList.add(opBean);
				}
				acidResp.setOperationList(operationList);
				responseList.add(acidResp);
			}
			entity.setResponseList(responseList);
		} catch (Exception e) {
			ErrorEntity errorEntity = new ErrorEntity();
			errorEntity.setCode(OfferAPConstantes.HTTP_SERVER_ERROR);
			String uidError = UUID.randomUUID().toString();
			errorEntity.setMessage("erreur de traitement côté serveur : " + uidError);
			Response response = Response.status(OfferAPConstantes.HTTP_SERVER_ERROR).entity(errorEntity).build();
			LOGGER.error(uidError + " : erreur lors de la récupération des opérations de cagnotte", e);
			return response;
		}
		return Response.ok().entity(entity).build();
	}

	@Override
	public Response requestMoneyTransfertByAcid(RequestMoneyTransfertByAcidRequest request) {
		if (testMode) {
			return requestMoneyTransfertByAcidMock(request);
		} else {
			return requestMoneyTransfertByAcidRecpg(request);
		}

	}

	private Response requestMoneyTransfertByAcidRecpg(RequestMoneyTransfertByAcidRequest request) {
		RequestMoneyTransfertByAcidResponse entity = new RequestMoneyTransfertByAcidResponse();
		if (request == null) {
			ErrorEntity errorEntity = new ErrorEntity();
			errorEntity.setCode(OfferAPConstantes.HTTP_MISSING_PARAMETERS);
			errorEntity.setMessage("missing request ");
			Response response = Response.status(OfferAPConstantes.HTTP_MISSING_PARAMETERS).entity(errorEntity).build();
			return response;
		}
		if (request.getTransfertList() == null || request.getTransfertList().isEmpty()) {
			ErrorEntity errorEntity = new ErrorEntity();
			errorEntity.setCode(OfferAPConstantes.HTTP_MISSING_PARAMETERS);
			errorEntity.setMessage("missing transfertList ");
			Response response = Response.status(OfferAPConstantes.HTTP_MISSING_PARAMETERS).entity(errorEntity).build();
			return response;
		}
		List<MoneyTransfertResponse> responseList = new ArrayList<MoneyTransfertResponse>();
		entity.setResponseList(responseList);
		RequestMoneyTransferRequest requestSoap = new RequestMoneyTransferRequest();
		for (MoneyTransfertAcid transfert : request.getTransfertList()) {
			MoneyTransfertResponse element = new MoneyTransfertResponse();
			if (transfert.getAcid() == null || transfert.getDate() == null || transfert.getHour() == null || transfert.getSupportAccount() == null) {
				element.setAcid(transfert.getAcid());
				element.setCode(MISSING_DATA);
			} else {
				element.setAcid(transfert.getAcid());
				try {
					requestSoap.setSource(OfferAPConstantes.APP_NAME);
					requestSoap.setBcid(transfert.getAcid());
					MoneyTransferRequest value = new MoneyTransferRequest();
					value.setDate(transfert.getDate());
					value.setTime(transfert.getHour());
					Boolean continueProcess = true;
					try {
						if (transfert.getSupportAccount() != null) {
							TargetIban targetIban = new TargetIban();
							StringTokenizer stToken = new StringTokenizer(transfert.getSupportAccount(), ";");
							while (stToken.hasMoreElements()) {
								Entry entry = new Entry();
								String[] keyValue = stToken.nextToken().split("=");
								entry.setKey(keyValue[0]);
								entry.setValue(keyValue[1]);
								targetIban.getEntry().add(entry);
							}
							value.setTargetIban(targetIban);
						} else {
							element.setCode(BAD_ACCOUNT_SUPPORT);
							continueProcess = false;
						}
					} catch (Exception e) {
						element.setCode(BAD_ACCOUNT_SUPPORT);
						continueProcess = false;
					}
					if (continueProcess) {
						requestSoap.setTransfer(value);
						RequestMoneyTransferResponse response = publishingSoap.requestMoneyTransfer(requestSoap);
						if (response.getGlobalCode() == 0) {
							element.setCode(OK);
						} else {
							element.setCode(response.getGlobalCode());
						}
					}
				} catch (Exception e) {
					LOGGER.error("erreur lors d'une demande de transfert", e);
					element.setCode(KO);
				}
			}
			responseList.add(element);
		}
		return Response.ok().entity(entity).build();
	}

	private Response requestMoneyTransfertByAcidMock(RequestMoneyTransfertByAcidRequest request) {
		RequestMoneyTransfertByAcidResponse entity = new RequestMoneyTransfertByAcidResponse();

		if (request == null) {
			ErrorEntity errorEntity = new ErrorEntity();
			errorEntity.setCode(OfferAPConstantes.HTTP_MISSING_PARAMETERS);
			errorEntity.setMessage("missing request ");
			Response response = Response.status(OfferAPConstantes.HTTP_MISSING_PARAMETERS).entity(errorEntity).build();
			return response;
		}
		if (request.getTransfertList() == null || request.getTransfertList().isEmpty()) {
			ErrorEntity errorEntity = new ErrorEntity();
			errorEntity.setCode(OfferAPConstantes.HTTP_MISSING_PARAMETERS);
			errorEntity.setMessage("missing transfertList ");
			Response response = Response.status(OfferAPConstantes.HTTP_MISSING_PARAMETERS).entity(errorEntity).build();
			return response;
		}
		List<MoneyTransfertResponse> responseList = new ArrayList<MoneyTransfertResponse>();
		entity.setResponseList(responseList);
		for (MoneyTransfertAcid transfert : request.getTransfertList()) {
			MoneyTransfertResponse element = new MoneyTransfertResponse();
			if (transfert.getAcid() == null || transfert.getDate() == null || transfert.getHour() == null || transfert.getSupportAccount() == null) {
				element.setAcid(transfert.getAcid());
				element.setCode(MISSING_DATA);
			} else {
				element.setAcid(transfert.getAcid());
				element.setCode(OK);
			}
			responseList.add(element);
		}
		return Response.ok().entity(entity).build();
	}

	public void setAcidDao(IAcidDao acidDao) {
		this.acidDao = acidDao;
	}

	public void setAccountOperationAcidDao(IAccountOperationAcidDao accountOperationAcidDao) {
		this.accountOperationAcidDao = accountOperationAcidDao;
	}

	public void setTestMode(Boolean testMode) {
		this.testMode = testMode;
		if (testMode == null) {
			this.testMode = false;
		}
	}

	public void setPublishingSoap(PublishingSoap publishingSoap) {
		this.publishingSoap = publishingSoap;
	}

}
