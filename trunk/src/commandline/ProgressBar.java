/*
   snuck - progress bar
   --------------------------------
   
   Author: Mauro Gentile <gentile.mauro.mg@gmail.com>

   Copyright 2012 Mauro Gentile

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package commandline;

public class ProgressBar {
    private StringBuilder progress;
    private boolean broken = false;
    
    public ProgressBar() {
        init();
    }

    public void update(int done, int total) {
        char[] workchars = {'|', '/', '-', '\\'};
        String format = "\r%s%3d%% %s %c";

        int percent = (++done * 100) / total;
        int extrachars = (percent / 2) - this.progress.length();

        while (extrachars-- > 0) {
            progress.append('#');
        }

        System.out.printf(format, (broken ? "[!]" : "   "), percent, progress,
        		workchars[done % workchars.length]);

        if (done == total) {
            System.out.flush();
            System.out.println();
            init();
        }
    }

    private void init() {
        this.progress = new StringBuilder(60);
    }
    
    public void setBroken() {
        this.broken = true;
    }
    
    public boolean getBroken() {
        return this.broken;
    }
}