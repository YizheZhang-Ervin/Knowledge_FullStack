package com.ervin.middleware.hdfs.controller;

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import com.ervin.middleware.hdfs.exception.HdfsApiException;
import com.ervin.middleware.hdfs.model.HDFSOp;
import com.ervin.middleware.hdfs.result.ResponseMessage;
import com.ervin.middleware.hdfs.result.ResponseResult;
import org.apache.hadoop.conf.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ervin.middleware.hdfs.hdfs.HdfsApi;
import com.ervin.middleware.hdfs.hdfs.service.HdfsApiService;
import com.ervin.middleware.hdfs.model.HDFSFileStatus;
import com.ervin.middleware.hdfs.model.ListFilter;
import com.ervin.middleware.hdfs.model.OP;
import com.ervin.middleware.hdfs.paging.DPage;
import com.ervin.middleware.hdfs.result.ResultData;

@CrossOrigin
@RestController
@RequestMapping("/ervin/webhdfs/v1")
public class HdfsApiController {

	@Autowired
	private Configuration conf;

	@Value("${hadoop.hdfs.user}")
	private String user;

	@Autowired
	private HdfsApiService apiService;

	@PostMapping
	public ResponseResult oPHdfs(@RequestBody HDFSOp hdfsOp, HttpServletResponse response) throws Exception {

		/**
		 * 自定义扩展
		 */

		boolean result = false;
		HdfsApi api = new HdfsApi(conf, user);
		OP op = hdfsOp.getOp();
		if (op == null) {
			throw new HdfsApiException("无法接收文件操作标识为空的请求");
		}

		if (op.equals(OP.CREATE)) {
			result = apiService.create(api, hdfsOp);
		} else if (op.equals(OP.DELETE)) {
			result = apiService.delete(api, hdfsOp);
		} else if (op.equals(OP.COPY)) {
			result = apiService.copy(api, hdfsOp);
		} else if (op.equals(OP.EMPTYTRASH)) {
			result = apiService.emptyTrash(api);
		} else if (op.equals(OP.MOVE)) {
			result = apiService.move(api, hdfsOp);
		} else if (op.equals(OP.RENAME)) {
			result = apiService.rename(api, hdfsOp);
		} else if (op.equals(OP.WRITE)) {
			result = apiService.write(api, hdfsOp);
		} else if (op.equals(OP.APPEND)) {
			result = apiService.append(api, hdfsOp);
		} else if (op.equals(OP.OPEN)) {
			apiService.open(api, hdfsOp, response);
		} 
		api.close();

		if (result) {
			return new ResponseResult(ResponseMessage.OK);
		} else {
			return new ResponseResult(ResponseMessage.INTERNAL_SERVER_ERROR);
		}
					
	}

	
	/**
	 * 上传文件
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	@PostMapping("/upload")
	public ResponseResult upLoadFile(@RequestParam(name = "file", required = true) MultipartFile file, 
			@RequestParam(name = "destPath") String destPath)
			throws Exception {
		HdfsApi api = new HdfsApi(conf, user);
		InputStream is = file.getInputStream();
		String name = file.getOriginalFilename();
		api.upLoadFile(is, destPath + "/" + name);
		api.close();
		return new ResponseResult(ResponseMessage.OK);
	}

	/**
	 * 下载文件
	 * 
	 * @return
	 */
	@GetMapping("/download")
	public ResponseResult downLoadFile(@RequestParam(name = "srcPath") String srcPath,
			HttpServletResponse response) throws Exception {
		HdfsApi api = new HdfsApi(conf, user);
		api.downLoadFile(srcPath, response, true);
		api.close();
		return new ResponseResult(ResponseMessage.OK);
	}
	
	
	@GetMapping
	public ResponseResult getFileStatus(ListFilter listFilter) throws Exception {

		HdfsApi api = new HdfsApi(conf, user);
	    OP op=OP.getEnum(listFilter.getOp());
		if (op == null) {
			throw new HdfsApiException("无法接收文件操作标识为空的请求");
		}

		DPage<HDFSFileStatus> dPage = null;

		if (op.equals(OP.FILElIST)) {
			dPage = apiService.getFileListStatus(api, listFilter);
		} else if (op.equals(OP.HOMELIST)) {
			dPage = apiService.getHomeListStatus(api, listFilter);
		} else if (op.equals(OP.TRASHLIST)) {
			dPage = apiService.getTrashListStatus(api, listFilter);
		}

		/**
		 * 构造返回结果
		 */
		ResultData<HDFSFileStatus> resultData = new ResultData<>(ResponseMessage.OK, dPage);

		api.close();
		return new ResponseResult(resultData);
	}

}
