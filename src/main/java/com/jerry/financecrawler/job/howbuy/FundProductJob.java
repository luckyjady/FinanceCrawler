package com.jerry.financecrawler.job.howbuy;import com.jerry.financecrawler.commons.CommonsCharset;import com.jerry.financecrawler.commons.CommonsUrl;import com.jerry.financecrawler.commons.ProductFilter;import com.jerry.financecrawler.db.dao.IFundProductDao;import com.jerry.financecrawler.db.dao.IIncomeDao;import com.jerry.financecrawler.db.po.FundProductPo;import com.jerry.financecrawler.db.po.IncomePo;import com.jerry.financecrawler.job.QuartzJob;import com.jerry.financecrawler.translate.howbuy.HtmlToFundProductVo;import com.jerry.financecrawler.visitor.HtmlRequest;import com.jerry.financecrawler.vo.FundProductTotalVo;import com.jerry.financecrawler.vo.FundProductVo;import com.jerry.financecrawler.vo.IncomeVo;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import javax.annotation.Resource;import java.util.List;/** * Created by Jerry on 2015/9/17. */public class FundProductJob implements QuartzJob {    /**     * Logger     */    private static final Logger log = LoggerFactory.getLogger(FundProductJob.class);    private static final String baseUrl = CommonsUrl.HOW_BUY_FINANCE_URL;    private static final String charset = CommonsCharset.UTF_8;    @Resource    private HtmlRequest htmlRequest;    @Resource    private HtmlToFundProductVo htmlToFundProductVo;    @Resource    private IFundProductDao fundProductDao;    @Resource    private IIncomeDao incomeDao;    @Override    public void execute() {        log.info("howbuy 基金产品采集服务启动");        try {            int index = 1;            int pageNum = 100;            FundProductTotalVo fundProductTotalVo = getHtmlData(index, pageNum, 0);            if (fundProductTotalVo != null) {                int allPages = fundProductTotalVo.getAllPages();                index++;                while (index <= allPages) {                    getHtmlData(index, pageNum, allPages);                    index++;                }            }        } catch (Exception ex) {            ex.printStackTrace();            log.error("Error " + this.getClass().getName() + "dealing EastFinance data", ex);        }        log.info("howbuy 基金产品采集服务完成");    }    private FundProductTotalVo getHtmlData(int index, int pageNum, int allpages) throws Exception {        String url = CommonsUrl.getUrl(baseUrl, index, pageNum, 0);        String html = htmlRequest.getHtmlData(url, charset);        FundProductTotalVo fundProductTotalVo = null;        if (!html.equals("")) {            fundProductTotalVo = htmlToFundProductVo.parseToFundProductData(html);            // System.out.println("eastFinanceTotalVo = ["+eastFinanceTotalVo.toString()+"]");            if (fundProductTotalVo != null) {                List<FundProductVo> fundProductVoList = fundProductTotalVo.getDatas();                saveFundProductData(fundProductVoList);            }        }        return fundProductTotalVo;    }    private void saveFundProductData(List<FundProductVo> fundProductVoList) {        if (fundProductVoList != null) {            Integer maxId = fundProductDao.getMaxId();            if (maxId == null) maxId = 0;            for (int i = 0; i < fundProductVoList.size(); i++) {                FundProductVo midVo = fundProductVoList.get(i);                String product_code = midVo.getProduct_code();                String product_name = midVo.getProduct_name();                String product_type = midVo.getProduct_TYPE();                if (ProductFilter.filter(product_type, product_type)) {                    FundProductPo basePo = fundProductDao.findByCodeOrName(product_code, product_name);                    if (basePo != null) {                        setBasePoValue(basePo, midVo);                        fundProductDao.modify(basePo);                        System.out.println("basePo = [" + basePo + "]");                        // 收益                        IncomePo baseIncomePo = incomeDao.find(basePo.getId());                        if (baseIncomePo != null) {                            setBaseIncomePoValue(baseIncomePo, midVo.getIncomeVo());                            incomeDao.modify(baseIncomePo);                        } else {                            FundProductPo midPo = changeToPo(midVo);                            IncomePo incomePo =midPo.getIncomePo();                            incomePo.setProduct_id(basePo.getId());                            incomeDao.save(incomePo);                        }                    } else {                        FundProductPo midPo = changeToPo(midVo);                        midPo.setId(maxId + i + 1);                        fundProductDao.save(midPo);                        IncomePo incomePo =midPo.getIncomePo();                        incomePo.setProduct_id(midPo.getId());                        // 收益                        incomeDao.save(incomePo);                    }                }//if            }// for        }// if    }    private void setBaseIncomePoValue(IncomePo baseIncomePo, IncomeVo incomeVo) {        if (baseIncomePo.getI_UPDATE_DATE() == null || baseIncomePo.getI_UPDATE_DATE().equals("")){            if (incomeVo.getI_UPDATE_DATE() == null || incomeVo.getI_UPDATE_DATE().equals("")){                baseIncomePo.setI_UPDATE_DATE("");            }else{                baseIncomePo.setI_UPDATE_DATE(incomeVo.getI_UPDATE_DATE());            }        }        if (baseIncomePo.getI_LATEST_NET_WORTH() == 0)            baseIncomePo.setI_LATEST_NET_WORTH(incomeVo.getI_LATEST_NET_WORTH());// 最新净值        if (baseIncomePo.getI_SINCE_THIS_YEAR() == 0)            baseIncomePo.setI_SINCE_THIS_YEAR(incomeVo.getI_SINCE_THIS_YEAR());// 今年以来        if (baseIncomePo.getI_NEARLY_A_MONTH() == 0)            baseIncomePo.setI_NEARLY_A_MONTH(incomeVo.getI_NEARLY_A_MONTH());// 近一月        if (baseIncomePo.getI_NEARLY_THREE_MONTHS() == 0)            baseIncomePo.setI_NEARLY_THREE_MONTHS(incomeVo.getI_NEARLY_THREE_MONTHS());// 近三月        if (baseIncomePo.getI_NEARLY_HALF_A_YEAR() == 0)            baseIncomePo.setI_NEARLY_HALF_A_YEAR(incomeVo.getI_NEARLY_HALF_A_YEAR());//近半年        if (baseIncomePo.getI_NEARLY_A_YEAR() == 0)            baseIncomePo.setI_NEARLY_A_YEAR(incomeVo.getI_NEARLY_A_YEAR());// 近一年        if (baseIncomePo.getI_NEARLY_TWO_YEARS() == 0)            baseIncomePo.setI_NEARLY_TWO_YEARS(incomeVo.getI_NEARLY_TWO_YEARS());//近两年        if (baseIncomePo.getI_NEARLY_THREE_YEARS() == 0)            baseIncomePo.setI_NEARLY_THREE_YEARS(incomeVo.getI_NEARLY_THREE_YEARS());//近三年        if (baseIncomePo.getI_NEARLY_FIVE_YEARS() == 0)            baseIncomePo.setI_NEARLY_FIVE_YEARS(incomeVo.getI_NEARLY_FIVE_YEARS());//近5年        if (baseIncomePo.getI_SINCE_ITS_ESTABLISHMENT() == 0)            baseIncomePo.setI_SINCE_ITS_ESTABLISHMENT(incomeVo.getI_SINCE_ITS_ESTABLISHMENT());//成立以来    }    private void setBasePoValue(FundProductPo basePo, FundProductVo vo) {        if (basePo.getProduct_name() == null || basePo.getProduct_name().equals(""))            basePo.setProduct_name(vo.getProduct_name()); //产品名称        if (basePo.getProduct_shortname() == null || basePo.getProduct_shortname().equals(""))            basePo.setProduct_shortname(vo.getProduct_shortname());//产品简称        if (basePo.getProduct_code() == null || basePo.getProduct_code().equals(""))            basePo.setProduct_code(vo.getProduct_code());//产品代码        if (basePo.getIsactice() == null || basePo.getIsactice().equals("")) basePo.setIsactice(vo.getIsactice());//产品状态        if (basePo.getSupplier_code() == null || basePo.getSupplier_code().equals(""))            basePo.setSupplier_code(vo.getSupplier_code());//基金发行人        if (basePo.getProduct_nature() == null || basePo.getProduct_nature().equals(""))            basePo.setProduct_nature(vo.getProduct_nature()); //基金性质        if (basePo.getProduct_bank() == null || basePo.getProduct_bank().equals(""))            basePo.setProduct_bank(vo.getProduct_bank());  //托管行        if (basePo.getProduct_COOPERATIVE_SECURITIES_INSTITUTION() == null || basePo.getProduct_COOPERATIVE_SECURITIES_INSTITUTION().equals(""))            basePo.setProduct_COOPERATIVE_SECURITIES_INSTITUTION(vo.getProduct_COOPERATIVE_SECURITIES_INSTITUTION());   //合作证券机构        if (basePo.getProduct_COOPERATIVE_FUTURES_AGENCY() == null || basePo.getProduct_COOPERATIVE_FUTURES_AGENCY().equals(""))            basePo.setProduct_COOPERATIVE_FUTURES_AGENCY(vo.getProduct_COOPERATIVE_FUTURES_AGENCY()); //合作期货机构        if (basePo.getProduct_ESTABLISHMENT_DATE() == null || basePo.getProduct_ESTABLISHMENT_DATE().equals(""))            basePo.setProduct_ESTABLISHMENT_DATE(vo.getProduct_ESTABLISHMENT_DATE()); //   date 成立日期        if (basePo.getProduct_DURATION_YEAR() == 0)            basePo.setProduct_DURATION_YEAR(vo.getProduct_DURATION_YEAR());    //存续期间(年)        if (basePo.getProduct_DURATION_EXISTENCE_YEAR() == 0)            basePo.setProduct_DURATION_EXISTENCE_YEAR(vo.getProduct_DURATION_EXISTENCE_YEAR());  //存续期限(年)        if (basePo.getProduct_TERMINATION_CONTION() == null || basePo.getProduct_TERMINATION_CONTION().equals(""))            basePo.setProduct_TERMINATION_CONTION(vo.getProduct_TERMINATION_CONTION());  //终止条件        if (basePo.getProduct_TYPE() == null || basePo.getProduct_TYPE().equals(""))            basePo.setProduct_TYPE(vo.getProduct_TYPE());   //基金类型        if (basePo.getProduct_INVESTMENT_TYPE() == null || basePo.getProduct_INVESTMENT_TYPE().equals(""))            basePo.setProduct_INVESTMENT_TYPE(vo.getProduct_INVESTMENT_TYPE());   //投资类型        if (basePo.getProduct_INVESTMENT_TYPE_DETAIL() == null || basePo.getProduct_INVESTMENT_TYPE_DETAIL().equals(""))            basePo.setProduct_INVESTMENT_TYPE_DETAIL(vo.getProduct_INVESTMENT_TYPE_DETAIL());   //投资类型细分        if (basePo.getProduct_info() == null || basePo.getProduct_info().equals(""))            basePo.setProduct_info(vo.getProduct_info());    // 产品信息        if (basePo.getProduct_INVESTMENT_TARGETS() == null || basePo.getProduct_INVESTMENT_TARGETS().equals(""))            basePo.setProduct_INVESTMENT_TARGETS(vo.getProduct_INVESTMENT_TARGETS());   //投资标的        if (basePo.getProduct_INVESTMENT_RATIO() == null || basePo.getProduct_INVESTMENT_RATIO().equals(""))            basePo.setProduct_INVESTMENT_RATIO(vo.getProduct_INVESTMENT_RATIO());    //投资比例        if (basePo.getProduct_EXPECTED_RETURN() == 0.0)            basePo.setProduct_EXPECTED_RETURN(vo.getProduct_EXPECTED_RETURN());  //预期收益        if (basePo.getProduct_HEDGE_RATIO() == 0.0)            basePo.setProduct_HEDGE_RATIO(vo.getProduct_HEDGE_RATIO());    //对冲比例        if (basePo.getProduct_INVESTMENT_THRESHOLD() == 0)            basePo.setProduct_INVESTMENT_THRESHOLD(vo.getProduct_INVESTMENT_THRESHOLD());    //投资门槛（万）        if (basePo.getProduct_ADDITIONAL_AMOUNT() == 0)            basePo.setProduct_ADDITIONAL_AMOUNT(vo.getProduct_ADDITIONAL_AMOUNT());   //追加金额（万）        if (basePo.getProduct_OPEN_PERIOD() == null || basePo.getProduct_OPEN_PERIOD().equals(""))            basePo.setProduct_OPEN_PERIOD(vo.getProduct_OPEN_PERIOD());    //开放期        if (basePo.getProduct_OPEN_FREQUENCY() == null || basePo.getProduct_OPEN_FREQUENCY().equals(""))            basePo.setProduct_OPEN_FREQUENCY(vo.getProduct_OPEN_FREQUENCY());   //开放频度        if (basePo.getProduct_CLOSURE_PERIOD() == 0)            basePo.setProduct_CLOSURE_PERIOD(vo.getProduct_CLOSURE_PERIOD());    //封闭期(天)（转换下）        if (basePo.getProduct_EARLY_WARNING_LINE() == 0.0)            basePo.setProduct_EARLY_WARNING_LINE(vo.getProduct_EARLY_WARNING_LINE());    //预警线        if (basePo.getProduct_STOP_LINE() == 0.0) basePo.setProduct_STOP_LINE(vo.getProduct_STOP_LINE());    //止损线        if (basePo.getProduct_CLASSIFICATON() == null || basePo.getProduct_CLASSIFICATON().equals(""))            basePo.setProduct_CLASSIFICATON(vo.getProduct_CLASSIFICATON());    //是否分级 0 否 1 是        if (basePo.getProduct_GRADING_SCALE() == null || basePo.getProduct_GRADING_SCALE().equals(""))            basePo.setProduct_GRADING_SCALE(vo.getProduct_GRADING_SCALE());    //分级比例        if (basePo.getProduct_OTHERS() == null || basePo.getProduct_OTHERS().equals(""))            basePo.setProduct_OTHERS(vo.getProduct_OTHERS()); //其他说明        if (basePo.getProduct_price() == 0.0) basePo.setProduct_price(vo.getProduct_price());   //产品价格        if (basePo.getCreatedate() == null || basePo.getCreatedate().equals(""))            basePo.setCreatedate(vo.getCreatedate());    //datetime        if (basePo.getProductcategory_code() == null || basePo.getProductcategory_code().equals(""))            basePo.setProductcategory_code(vo.getProductcategory_code());  //产品分类代码        if (basePo.getProduct_image_url() == null || basePo.getProduct_image_url().equals(""))            basePo.setProduct_image_url(vo.getProduct_image_url());    //图片url        if (basePo.getProduct_is_crawler() == 0)            basePo.setProduct_is_crawler(vo.getProduct_is_crawler()); //是否为爬取 1 是 0 不是    }    private FundProductPo changeToPo(FundProductVo vo) {        //产品        FundProductPo po = new FundProductPo();        po.setId(vo.getId());        po.setProduct_name(vo.getProduct_name()); //产品名称        po.setProduct_shortname(vo.getProduct_shortname());//产品简称        po.setProduct_code(vo.getProduct_code());//产品代码        po.setIsactice(vo.getIsactice());//产品状态        po.setSupplier_code(vo.getSupplier_code());//基金发行人        po.setProduct_nature(vo.getProduct_nature()); //基金性质        po.setProduct_bank(vo.getProduct_bank());  //托管行        po.setProduct_COOPERATIVE_SECURITIES_INSTITUTION(vo.getProduct_COOPERATIVE_SECURITIES_INSTITUTION());   //合作证券机构        po.setProduct_COOPERATIVE_FUTURES_AGENCY(vo.getProduct_COOPERATIVE_FUTURES_AGENCY()); //合作期货机构        po.setProduct_ESTABLISHMENT_DATE(vo.getProduct_ESTABLISHMENT_DATE()); //   date 成立日期        po.setProduct_DURATION_YEAR(vo.getProduct_DURATION_YEAR());    //存续期间(年)        po.setProduct_DURATION_EXISTENCE_YEAR(vo.getProduct_DURATION_EXISTENCE_YEAR());  //存续期限(年)        po.setProduct_TERMINATION_CONTION(vo.getProduct_TERMINATION_CONTION());  //终止条件        po.setProduct_TYPE(vo.getProduct_TYPE());   //基金类型        po.setProduct_INVESTMENT_TYPE(vo.getProduct_INVESTMENT_TYPE());   //投资类型        po.setProduct_INVESTMENT_TYPE_DETAIL(vo.getProduct_INVESTMENT_TYPE_DETAIL());   //投资类型细分        po.setProduct_info(vo.getProduct_info());    // 产品信息        po.setProduct_INVESTMENT_TARGETS(vo.getProduct_INVESTMENT_TARGETS());   //投资标的        po.setProduct_INVESTMENT_RATIO(vo.getProduct_INVESTMENT_RATIO());    //投资比例        po.setProduct_EXPECTED_RETURN(vo.getProduct_EXPECTED_RETURN());  //预期收益        po.setProduct_HEDGE_RATIO(vo.getProduct_HEDGE_RATIO());    //对冲比例        po.setProduct_INVESTMENT_THRESHOLD(vo.getProduct_INVESTMENT_THRESHOLD());    //投资门槛（万）        po.setProduct_ADDITIONAL_AMOUNT(vo.getProduct_ADDITIONAL_AMOUNT());   //追加金额（万）        po.setProduct_OPEN_PERIOD(vo.getProduct_OPEN_PERIOD());    //开放期        po.setProduct_OPEN_FREQUENCY(vo.getProduct_OPEN_FREQUENCY());   //开放频度        po.setProduct_CLOSURE_PERIOD(vo.getProduct_CLOSURE_PERIOD());    //封闭期(天)（转换下）        po.setProduct_EARLY_WARNING_LINE(vo.getProduct_EARLY_WARNING_LINE());    //预警线        po.setProduct_STOP_LINE(vo.getProduct_STOP_LINE());    //止损线        po.setProduct_CLASSIFICATON(vo.getProduct_CLASSIFICATON());    //是否分级 0 否 1 是        po.setProduct_GRADING_SCALE(vo.getProduct_GRADING_SCALE());    //分级比例        po.setProduct_OTHERS(vo.getProduct_OTHERS()); //其他说明        po.setProduct_price(vo.getProduct_price());   //产品价格        po.setCreatedate(vo.getCreatedate());    //datetime        po.setProductcategory_code(vo.getProductcategory_code());  //产品分类代码        po.setProduct_image_url(vo.getProduct_image_url());    //图片url        po.setProduct_is_crawler(1); //是否为爬取 1 是 0 不是        //收益        IncomeVo incomeVo = vo.getIncomeVo();        IncomePo incomePo = new IncomePo();        incomePo.setProduct_id(incomeVo.getProduct_id());        incomePo.setI_UPDATE_DATE(incomeVo.getI_UPDATE_DATE());// date 更新时间        incomePo.setI_LATEST_NET_WORTH(incomeVo.getI_LATEST_NET_WORTH());// 最新净值        incomePo.setI_SINCE_THIS_YEAR(incomeVo.getI_SINCE_THIS_YEAR());// 今年以来        incomePo.setI_NEARLY_A_MONTH(incomeVo.getI_NEARLY_A_MONTH());// 近一月        incomePo.setI_NEARLY_THREE_MONTHS(incomeVo.getI_NEARLY_THREE_MONTHS());// 近三月        incomePo.setI_NEARLY_HALF_A_YEAR(incomeVo.getI_NEARLY_HALF_A_YEAR());//近半年        incomePo.setI_NEARLY_A_YEAR(incomeVo.getI_NEARLY_A_YEAR());// 近一年        incomePo.setI_NEARLY_TWO_YEARS(incomeVo.getI_NEARLY_TWO_YEARS());//近两年        incomePo.setI_NEARLY_THREE_YEARS(incomeVo.getI_NEARLY_THREE_YEARS());//近三年        incomePo.setI_NEARLY_FIVE_YEARS(incomeVo.getI_NEARLY_FIVE_YEARS());//近5年        incomePo.setI_SINCE_ITS_ESTABLISHMENT(incomeVo.getI_SINCE_ITS_ESTABLISHMENT());//成立以来        po.setIncomePo(incomePo);        return po;    }}