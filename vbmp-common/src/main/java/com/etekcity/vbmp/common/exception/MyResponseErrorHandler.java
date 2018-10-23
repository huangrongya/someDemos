package com.etekcity.vbmp.common.exception;

import java.io.IOException;
import java.nio.charset.Charset;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.UnknownHttpStatusCodeException;

@Slf4j
public class MyResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = getHttpStatusCode(response);
        switch (statusCode.series()) {
            case CLIENT_ERROR:
                //throw new HttpClientErrorException(statusCode, response.getStatusText(),
                //response.getHeaders(), getResponseBody(response), getCharset(response));
            case SERVER_ERROR:
                //throw new HttpServerErrorException(statusCode, response.getStatusText(),
                //response.getHeaders(), getResponseBody(response), getCharset(response));
            default:
                throw new UnknownHttpStatusCodeException(statusCode.value(), response.getStatusText(),
                        response.getHeaders(), getResponseBody(response), getCharset(response));
        }
    }

    /**
     * Determine the HTTP status of the given response.
     * <p>Note: Only called from {@link #handleError}, not from {@link #hasError}.
     *
     * @param response the response to inspect
     * @return the associated HTTP status
     * @throws IOException                    in case of I/O errors
     * @throws UnknownHttpStatusCodeException in case of an unknown status code
     *                                        that cannot be represented with the {@link HttpStatus} enum
     * @since 4.3.8
     */
    protected HttpStatus getHttpStatusCode(ClientHttpResponse response) throws IOException {
        try {
            return response.getStatusCode();
        } catch (IllegalArgumentException ex) {
            throw new UnknownHttpStatusCodeException(response.getRawStatusCode(), response.getStatusText(),
                    response.getHeaders(), getResponseBody(response), getCharset(response));
        }
    }

    /**
     * Read the body of the given response (for inclusion in a status exception).
     *
     * @param response the response to inspect
     * @return the response body as a byte array,
     * or an empty byte array if the body could not be read
     * @since 4.3.8
     */
    protected byte[] getResponseBody(ClientHttpResponse response) {
        try {
            return FileCopyUtils.copyToByteArray(response.getBody());
        } catch (IOException ex) {
            // ignore
        }
        return new byte[0];
    }

    /**
     * Determine the charset of the response (for inclusion in a status exception).
     *
     * @param response the response to inspect
     * @return the associated charset, or {@code null} if none
     * @since 4.3.8
     */
    protected Charset getCharset(ClientHttpResponse response) {
        HttpHeaders headers = response.getHeaders();
        MediaType contentType = headers.getContentType();
        return (contentType != null ? contentType.getCharset() : null);
    }

    @Override
    public boolean hasError(ClientHttpResponse clienthttpresponse) throws IOException {
        int rawStatusCode = clienthttpresponse.getRawStatusCode();
        for (HttpStatus statusCode : HttpStatus.values()) {
            if (statusCode.value() == rawStatusCode) {
                return hasError(statusCode);
            }
        }
        return false;
    }

    /**
     * Template method called from {@link #hasError(ClientHttpResponse)}.
     * <p>The default implementation checks if the given status code is
     * {@link HttpStatus.Series#CLIENT_ERROR CLIENT_ERROR} or
     * {@link HttpStatus.Series#SERVER_ERROR SERVER_ERROR}.
     * Can be overridden in subclasses.
     *
     * @param statusCode the HTTP status code
     * @return {@code true} if the response has an error; {@code false} otherwise
     * @see #getHttpStatusCode(ClientHttpResponse)
     */
    protected boolean hasError(HttpStatus statusCode) {
        return (statusCode.series() == HttpStatus.Series.CLIENT_ERROR ||
                statusCode.series() == HttpStatus.Series.SERVER_ERROR);
    }
}
