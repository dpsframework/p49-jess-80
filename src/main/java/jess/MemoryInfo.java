package jess;

class MemoryInfo {
    int rightSlot = -1;
    int rightSubSlot = -1;
    int leftSlot = -1;
    int leftSubSlot = -1;
    int tokenIndex = 0;
    boolean blessed = false;

    MemoryInfo(TestBase[] tests, int nTests) {

        // Try to have a Test2Simple first
        for (int i = 0; i < nTests; i++) {
            TestBase t = tests[i];
            if (t instanceof Test2Simple) {
                Test2Simple t2s = (Test2Simple) t;

                if (t2s.getTest()) {
                    if (t2s.getRightIndex() == -1 ||
                            t2s.getLeftIndex() == -1)
                        continue;

                    if (i > 0) {
                        TestBase tmp = tests[0];
                        tests[0] = t2s;
                        tests[i] = tmp;
                    }

                    rightSlot = t2s.getRightIndex();
                    tokenIndex = t2s.getTokenIndex();
                    leftSlot = t2s.getLeftIndex();
                    blessed = true;
                    break;
                }
            }
        }


        // If this fails, try to have a Test2Multi first
        if (!blessed) {
            for (int i = 0; i < nTests; i++) {
                TestBase t = tests[i];
                if (t instanceof Test2Multi) {
                    Test2Multi t2s = (Test2Multi) t;

                    if (t2s.getTest()) {
                        if (t2s.getRightIndex() == -1 ||
                                t2s.getLeftIndex() == -1)
                            continue;

                        if (i > 0) {
                            TestBase tmp = tests[0];
                            tests[0] = t2s;
                            tests[i] = tmp;
                        }
                        rightSlot = t2s.getRightIndex();
                        rightSubSlot = t2s.getRightSubIndex();
                        tokenIndex = t2s.getTokenIndex();
                        leftSlot = t2s.getLeftIndex();
                        leftSubSlot = t2s.getLeftSubIndex();
                        blessed = true;
                        break;
                    }
                }
            }
        }
    }
}
