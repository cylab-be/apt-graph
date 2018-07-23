#!/usr/bin/env python
#
#
# Dependencies:
#   Linux, Python 2.6, Pyinotify :
#   sudo pip install pyinotify

import subprocess
import pyinotify
import os


class OnWriteHandler(pyinotify.ProcessEvent):
    def my_init(self, path, cmd):
        self.dir = path
        self.cmd = cmd

    def process_default(self, event):
        if event.pathname == self.dir:
            if event.maskname == 'IN_CLOSE_WRITE':
                print '==> Modification detected'
                subprocess.call(
                    self.cmd.split(' '),
                    cwd=os.path.dirname(self.dir),
                    stdout=subprocess.PIPE)


if __name__ == '__main__':

    path = os.path.join(os.getcwd(), 'paper.tex')
    cmd = 'make'

    wm = pyinotify.WatchManager()
    handler = OnWriteHandler(path=path, cmd=cmd)
    notifier = pyinotify.Notifier(wm, default_proc_fun=handler)
    wm.add_watch(os.path.dirname(path), pyinotify.ALL_EVENTS)

    print '==> Start monitoring %s (type c^c to exit)' % path
    notifier.loop()

    print 'Bye!'
