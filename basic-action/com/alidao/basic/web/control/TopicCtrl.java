package com.alidao.basic.web.control;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Option;
import com.alidao.basic.entity.Topic;
import com.alidao.basic.service.OptionService;
import com.alidao.basic.service.TopicService;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.PageParam;

@Controller
@RequestMapping("topic")
public class TopicCtrl extends WebCtrl {

	@Autowired
	private TopicService topicService;
	@Autowired
	private OptionService	optionService;
	@RequestMapping("init")
	public void init(String questionnaireId,Model model) {
		model.addAttribute("questionnaireId", questionnaireId);
	}

	@RequestMapping("page")
	public void page(PageParam pageParam, 
			Topic object, HttpServletResponse response) 
					throws Exception {
		topicService.page(pageParam, object).jsonOut(response);
	}


	@RequestMapping("input")
	public void edit(String id, Model model,String questionnaireId) {
		if (id != null) {
			Topic object = topicService.find(id);
			Option option=new Option();
			option.addOrderBy("seq");
			option.setTopicId(object.getId());
			List<Option> optionList=optionService.list(option);
			model.addAttribute("optionList", optionList);
			model.addAttribute("object", object);
		}
		model.addAttribute("questionnaireId", questionnaireId);
	}

	@RequestMapping("save")
	public void save(
			HttpServletResponse response, 
			Topic object,String [] ordinal,String [] optionName,Integer [] optionSeq)
			throws Exception {
		if (StringUtil.isEmpty(object.getId())) {
			getResponse(
				topicService.save(object, ordinal, optionName, optionSeq)
			).jsonOut(response);
		} else {
			getResponse(
				topicService.mdfy(object, ordinal,optionName,optionSeq)
			).jsonOut(response);
		}
	}
	
	@RequestMapping("lose/{id}")
	public void lose(
			@PathVariable("id") String id, 
			HttpServletResponse response)
			throws Exception {
		getResponse(
			topicService.lose(id)
		).jsonOut(response);
	}
}
