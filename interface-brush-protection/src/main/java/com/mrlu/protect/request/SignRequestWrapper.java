package com.mrlu.protect.request;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.TreeMap;

/**
 * @author 简单de快乐
 * @create 2023-06-05 11:35
 *
 *
 *
 */
@Slf4j
public class SignRequestWrapper extends HttpServletRequestWrapper {

    /**
     * request数据流只能读取一次，需要自己实现HttpServletRequestWrapper对数据流包装，目的是将request流保存下来
     */
    private byte[] requestBody;

//    public SignRequestWrapper(HttpServletRequest request) throws IOException {
//        super(request);
//
////        Map<String, String[]> parameterMap = request.getParameterMap();
////        System.out.println(parameterMap);
//
//        // 【注意】
//        // post请求application/x-www-form-urlencoded提交 request.getInputStream()获取不到请求参数。
//        // 只能通过request.getParameterMap();request.getParameter();request.getParameterNames();request.getParameterValues();方法来协助获取
//        // 那为什么调用request.getParameterMap()方法，下面的request.getInputStream()就返回空流呢？
//        // 因为request.getParameterMap()底层代码实际上调用了
//        /*public Map<String, String[]> getParameterMap() {
//            if (this.parameterMap.isLocked()) {
//                return this.parameterMap;
//            } else {
//                // 从这debug进去
//                Enumeration enumeration = this.getParameterNames();
//
//                while(enumeration.hasMoreElements()) {
//                    String name = (String)enumeration.nextElement();
//                    String[] values = this.getParameterValues(name);
//                    this.parameterMap.put(name, values);
//                }
//
//                this.parameterMap.setLocked(true);
//                return this.parameterMap;
//            }
//        }*
//
//        // 一直debug进去到下面这个代码位置
//        if ("application/x-www-form-urlencoded".equals(contentType)) {
//              ......
//
//              try {
//                        // 然后从这里进去
//                        if (this.readPostBody(formData, len) != len) {
//                            parameters.setParseFailedReason(FailReason.REQUEST_BODY_INCOMPLETE);
//                            return;
//                        }
//                    } catch (IOException var19) {
//                        Context context = this.getContext();
//                        if (context != null && context.getLogger().isDebugEnabled()) {
//                            context.getLogger().debug(sm.getString("coyoteRequest.parseParameters"), var19);
//                        }
//
//                        parameters.setParseFailedReason(FailReason.CLIENT_DISCONNECT);
//                        return;
//                    }
//
//                    parameters.processParameters(formData, 0, len);
//                }
//
//        }
//
//        protected int readPostBody(byte[] body, int len) throws IOException {
//            int offset = 0;
//
//            do {
//                // debug进去getStream()方法
//                // 流在这里读取了一次，后续的request.getInputStream()都拿不到流了。
//                int inputLen = this.getStream().read(body, offset, len - offset);
//                if (inputLen <= 0) {
//                    return offset;
//                }
//
//                offset += inputLen;
//            } while(len - offset > 0);
//
//            return len;
//        }
//
//        // request.getInputStream() 实际也是使用了this.inputStream。
//        public InputStream getStream() {
//            if (this.inputStream == null) {
//                this.inputStream = new CoyoteInputStream(this.inputBuffer);
//            }
//            return this.inputStream;
//        }
//
//
//
//        一路看下来为什么我们的/sign/test03接口注入参数没有成功，因为
//         post请求以 application/x-www-form-urlencoded 提交时，
//         springmvc底层是用类似下面这端代码获取参数然后注入到接口的实体类的，
//         如果我们没有先调用request.getParameterMap()或者下面这段代码优先获取流，
//         当前构造方法只有requestBody = StreamUtils.copyToByteArray(request.getInputStream());获取了流，
//         就会导致springmvc底层就获取不到流了，Enumeration<String> paramNames 就是空。
//            Enumeration<String> paramNames = request.getParameterNames();
//            while (paramNames.hasMoreElements()) {
//                String paramName = paramNames.nextElement();
//                String value = request.getParameter(paramName);
//                paramMap.put(paramName, value);
//            }
//         */
//
//
//        /*
//         public ServletInputStream getInputStream() throws IOException {
//            if (this.usingReader) {
//                throw new IllegalStateException(sm.getString("coyoteRequest.getInputStream.ise"));
//            } else {
//
//                // 设置成true，然后request.getParameterNames();就会返回空
//                this.usingInputStream = true;
//                if (this.inputStream == null) {
//                    this.inputStream = new CoyoteInputStream(this.inputBuffer);
//                }
//
//                return this.inputStream;
//            }
//        }
//         */
//        requestBody = StreamUtils.copyToByteArray(request.getInputStream());
//
//
//        // post请求以 application/x-www-form-urlencoded 提交
//        // 如果先使用request.getInputStream()或者request.getReader()。以下4个方法就会获取不到请求参数
//        // Map<String, String[]> parameterMap = request.getParameterMap();
//        // String id1 = request.getParameter("id");
//        //  request.getParameterNames();
//        // String[] id2 = request.getParameterValues("id");
//        // System.out.println(parameterMap);
//        // System.out.println(id1);
//        // System.out.println(Arrays.toString(id2));
//        // 以下4个方法底层调用同一个方法。
//        /*
//       protected void parseParameters() {
//            this.parametersParsed = true;
//            Parameters parameters = this.coyoteRequest.getParameters();
//            boolean success = false;
//
//            try {
//                parameters.setLimit(this.getConnector().getMaxParameterCount());
//                Charset charset = this.getCharset();
//                boolean useBodyEncodingForURI = this.connector.getUseBodyEncodingForURI();
//                parameters.setCharset(charset);
//                if (useBodyEncodingForURI) {
//                    parameters.setQueryStringCharset(charset);
//                }
//
//                parameters.handleQueryParameters();
//                if (this.usingInputStream || this.usingReader) {
//                    // 使用了request.getInputStream();usingInputStream就会设置为true，就会直接结束。
//
//                    success = true;
//                    return;
//                }
//            }
//
//            .....
//        }
//        */
//
//
//        /**
//         * 总结：
//         * （1）当form表单内容采用 enctype=application/x-www-form-urlencoded编码时，
//         * 先通过调用 request.getParameterMap();
//         *         request.getParameter();
//         *         request.getParameterNames();
//         *         request.getParameterValues();方法得到参数后，
//         * 再调用request.getInputStream()或request.getReader()已经得不到流中的内容，
//         * 因为在调用 request.getParameterMap();
//         *           request.getParameter();
//         *           request.getParameterNames();
//         *           request.getParameterValues();
//         * 时系统可能对表单中提交的数据以流的形式读了一次,反之亦然。
//         *
//         *
//         * （2）当form表单内容采用enctype=multipart/form-data编码时，
//         *     也是同enctype=application/x-www-form-urlencoded一样的道理
//         *
//         * 参考：
//         * https://www.cnblogs.com/sfnz/p/16416039.html.
//         * 但是需要注意博客关于multipart/form-data时的request.getParameter()
//         *      方法和request.getInputStream()或request.getReader()获取结果的描述是错误的。
//         * request.getQueryString()获取get请求url后面的请求参数，如获取到：k1=v1&k2=v2
//         */
//
//    }

    public SignRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        init(request);
    }

    private void init(HttpServletRequest request) throws IOException {
        String header = request.getHeader("Content-Type");
        log.info("Content-Type={}", header);
        if (StringUtils.isNotEmpty(header)) {
            if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(header)) {
                // post application/x-www-form-urlencoded
                // 解决controller注入参数失败和校验是否重复请求失败


                String queryString = request.getQueryString();
                System.out.println("==queryString==>" +  queryString);

                setRequestBodyWithForm(request);
            } else if (header.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
                // post multipart/form-data
                // 解决controller注入参数失败和校验是否重复请求失败

                String queryString = request.getQueryString();
                System.out.println("==queryString==>" +  queryString);

                setRequestBodyWithForm(request);
            } else {
                requestBody = StreamUtils.copyToByteArray(request.getInputStream());
            }
        } else {
            requestBody = StreamUtils.copyToByteArray(request.getInputStream());
        }
    }

    private void setRequestBodyWithForm(HttpServletRequest request) {
        TreeMap<String, Object> paramMap = new TreeMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String value = request.getParameter(paramName);
            paramMap.put(paramName, value);
        }
        String params2Json = JSONObject.toJSONString(paramMap);
        requestBody = params2Json.getBytes();
    }


    @Override
    public ServletInputStream getInputStream() throws IOException {

        // 只能获取json形式传过来的
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(requestBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }
}
