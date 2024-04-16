package ru.ozon.api.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Commissions {
    @JsonProperty("sales_percent")
    private double salesPercent;
    @JsonProperty("fbo_fulfillment_amount")
    private double fboFulfillmentAmount;
    @JsonProperty("fbo_direct_flow_trans_min_amount")
    private double fboDirectFlowTransMinAmount;
    @JsonProperty("fbo_direct_flow_trans_max_amount")
    private double fboDirectFlowTransMaxAmount;
    @JsonProperty("fbo_deliv_to_customer_amount")
    private double fboDelivToCustomerAmount;
    @JsonProperty("fbo_return_flow_amount")
    private double fboReturnFlowAmount;
    @JsonProperty("fbo_return_flow_trans_min_amount")
    private double fboReturnFlowTransMinAmount;
    @JsonProperty("fbo_return_flow_trans_max_amount")
    private double fboReturnFlowTransMaxAmount;
    @JsonProperty("fbs_first_mile_min_amount")
    private double fbsFirstMileMinAmount;
    @JsonProperty("fbs_first_mile_max_amount")
    private double fbsFirstMileMaxAmount;
    @JsonProperty("fbs_direct_flow_trans_min_amount")
    private double fbsDirectFlowTransMinAmount;
    @JsonProperty("fbs_direct_flow_trans_max_amount")
    private double fbsDirectFlowTransMaxAmount;
    @JsonProperty("fbs_deliv_to_customer_amount")
    private double fbsDelivToCustomerAmount;
    @JsonProperty("fbs_return_flow_amount")
    private double fbsReturnFlowAmount;
    @JsonProperty("fbs_return_flow_trans_min_amount")
    private double fbsReturnFlowTransMinAmount;
    @JsonProperty("fbs_return_flow_trans_max_amount")
    private double fbsReturnFlowTransMaxAmount;
    @JsonProperty("sales_percent_fbo")
    private double salesPercentFbo;
    @JsonProperty("sales_percent_fbs")
    private double salesPercentFbs;
}
