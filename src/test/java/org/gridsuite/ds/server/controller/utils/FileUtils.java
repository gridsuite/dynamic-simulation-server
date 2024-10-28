/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.controller.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class FileUtils {
    private FileUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static void writeBytesToFile(Object caller, String filePathName, byte[] content) throws IOException {
        File file = new File(caller.getClass().getClassLoader().getResource(".").getFile() + filePathName);

        //re-entrant
        if (file.exists()) {
            file.delete();
        }

        file.createNewFile();
        OutputStream os = new FileOutputStream(file);
        os.write(content);
        os.close();
    }
}
