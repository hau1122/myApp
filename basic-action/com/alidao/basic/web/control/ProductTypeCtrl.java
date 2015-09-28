package com.alidao.basic.web.control;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.ProductType;
import com.alidao.basic.service.ProductTypeService;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.PageParam;
import com.alidao.jxe.model.ParamException;

@Controller
@RequestMapping("productType")
public class ProductTypeCtrl extends WebCtrl {

	@Autowired
	private ProductTypeService productTypeService;

	@RequestMapping("init")
	public void init() {
	}

	@RequestMapping("list")
	public void list(PageParam pageParam, ProductType object,
			HttpServletResponse response) throws IOException {
		productTypeService.page(pageParam, object).jsonOut(response);
	}

	@RequestMapping("edit")
	public void edit(Model model, String id) {
		if (StringUtil.isNotBlank(id)) {
			model.addAttribute("object", productTypeService.find(id));
		}
	}

	@RequestMapping("save")
	public void save(ProductType object, HttpServletResponse response)
			throws IOException, ParamException {
		if (StringUtil.isEmpty(object.getId())) {
			getResponse(productTypeService.save(object)).jsonOut(response);
		} else {
			getResponse(productTypeService.mdfy(object)).jsonOut(response);
		}
	}
	
	@RequestMapping("lose/{id}")
	public void lose(@PathVariable("id") String id, 
			HttpServletResponse response) throws IOException {
		getResponse(productTypeService.lose(id)).jsonOut(response);
	}
}
