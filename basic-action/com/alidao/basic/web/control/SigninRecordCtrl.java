package com.alidao.basic.web.control;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import com.alidao.basic.entity.SigninRecord;
import com.alidao.basic.service.SigninRecordService;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.PageParam;

@Controller
@RequestMapping("signinRecord")
public class SigninRecordCtrl extends WebCtrl {

	@Autowired
	private SigninRecordService signinRecordService;
	
	@RequestMapping("init")
	public void init() {
	}
	@RequestMapping("page")
	public void page(PageParam pageParam, 
			SigninRecord object, HttpServletResponse response) 
					throws Exception {
		signinRecordService.page(pageParam, object).jsonOut(response);
	}
}
