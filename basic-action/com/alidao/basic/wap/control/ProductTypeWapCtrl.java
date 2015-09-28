package com.alidao.basic.wap.control;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Advert;
import com.alidao.basic.entity.ProductType;
import com.alidao.basic.service.AdvertService;
import com.alidao.basic.service.ProductTypeService;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.PageParam;

@Controller
@RequestMapping("wap/ptype")
public class ProductTypeWapCtrl extends WebCtrl {

	@Autowired
	private ProductTypeService productTypeService;
	
	@Autowired
	private AdvertService advertService;
	
	@RequestMapping("list")
	public void list(Model model) {
		ProductType cdt = new ProductType();
		cdt.addOrderBy("seq");
		List<ProductType> list = productTypeService.list(cdt);
		Advert advert=new Advert();
		advert.setPosition(Advert.POSITION_TYPE_INDEX);
		PageParam pageParam=new PageParam();
		pageParam.setPageSize(3L);
		pageParam.setSortWay("weight");
		List<Advert> advertList=advertService.page(pageParam,advert).getTableList();
		model.addAttribute("advertList", advertList);
		model.addAttribute("list", list);
	}

}
