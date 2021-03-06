package com.paperight.mvc.controller;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.paperight.email.integration.EmailGateway;
import com.paperight.publisherearning.PublisherEarningService;
import com.paperight.publisherearning.PublisherPaymentRequest;
import com.paperight.publisherearning.PublisherPaymentRequestStatus;
import com.paperight.user.PublisherPaymentDetails;

@Controller
@PreAuthorize("hasPermission(#user, 'PROCESS_PUBLISHER_PAYMENT_REQUESTS')")
public class PublisherEarningController {
	
	private Logger logger = LoggerFactory.getLogger(PublisherEarningController.class);
	
	@Autowired
	private EmailGateway emailGateway;
	
	@Autowired
	private PublisherEarningService publisherEarningService;
	
	@RequestMapping(value="/publisher-payment-requests/search", method = RequestMethod.GET )
	public String searchPublisherPaymentRequests(@ModelAttribute PublisherPaymentRequestSearch publisherPaymentRequestSearch, Model model) {
		List<PublisherPaymentRequest> publisherPaymentRequests = PublisherPaymentRequest.findByStatus(publisherPaymentRequestSearch.getStatus());
//		model.addAttribute("publisherPaymentRequestSearch", publisherPaymentRequestSearch);
		model.addAttribute("publisherPaymentRequests", publisherPaymentRequests);
		return "publisher-payment-requests/search";
	}
	
	@RequestMapping(value="/publisher-payment-request/{id}/{action}")
	public String authorise(@PathVariable Long id, @PathVariable String action) {
		PublisherPaymentRequest publisherPaymentRequest  = PublisherPaymentRequest.find(id);
		String status = "";
		if (publisherPaymentRequest != null) {
			if (StringUtils.equalsIgnoreCase(action, "complete")) {
				publisherEarningService.completePublisherPaymentRequest(publisherPaymentRequest);
				status = PublisherPaymentRequestStatus.PAID.toString();
				emailGateway.paidPublisherPaymentRequest(publisherPaymentRequest);
			} else if (StringUtils.equalsIgnoreCase(action, "cancel")) {
				publisherEarningService.cancelPublisherPaymentRequest(publisherPaymentRequest);
				status = PublisherPaymentRequestStatus.CANCELLED.toString();
			}
		}
		return "redirect:/publisher-payment-requests/search?status=" + status;
	}
	
	@RequestMapping(value="/publisher-payment-request/{id}")
	public String viewPublisherPaymentRequest(@PathVariable Long id, Model model) {
		PublisherPaymentRequest publisherPaymentRequest = PublisherPaymentRequest.find(id);
		if (publisherPaymentRequest != null) {
			PublisherPaymentDetails publisherPaymentDetails = PublisherPaymentDetails.findByCompanyId(publisherPaymentRequest.getCompany().getId());
			model.addAttribute("publisherPaymentRequest", publisherPaymentRequest);
			model.addAttribute("publisherPaymentDetails", publisherPaymentDetails);
			return "publisher-payment-request/view";
		} else {
			return "redirect:/publisher-payment-requests/search";
		}
	}

}

class PublisherPaymentRequestSearch {
	
	private PublisherPaymentRequestStatus status = PublisherPaymentRequestStatus.PENDING;

	public PublisherPaymentRequestStatus getStatus() {
		return status;
	}

	public void setStatus(PublisherPaymentRequestStatus status) {
		this.status = status;
	} 
	
}
