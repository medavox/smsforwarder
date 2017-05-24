package com.medavox.smsforwarder;

/**@author Adam Howard
 * @date 18/05/2017*/
/**Sending files will be so slow, that there's no point in a queue for received commands.
 * We'll just listen for download requests until we receive one,
 * then only listen for cancel requests (while we send the file in tiny pieces).
 *
 * a file transfer:
 * filename
 * source URL
 * size
 * mime type
 * pieces
 *
 * run the html through
 * minification
 * conversion of html into a briefer format
 * compression
 * removal of SEO meta tags etc
 * removal of internationalisation data
 *
 * each piece:
 * piece number
 * data */
public class SmsSender {
    static {

    }
}
