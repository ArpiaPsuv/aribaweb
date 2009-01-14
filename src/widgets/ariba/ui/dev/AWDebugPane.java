/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWDebugPane.java#25 $
*/
package ariba.ui.dev;

import ariba.ui.widgets.AribaPageContent;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBaseResponse;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWConstants;
import ariba.ui.aribaweb.core.AWValidationContext;
import ariba.ui.aribaweb.core.AWComponentDefinition;
import ariba.ui.aribaweb.core.AWComponentApiManager;
import ariba.ui.aribaweb.core.AWComponentActionRequestHandler;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.AWNamespaceManager;
import ariba.util.core.PerformanceChecker;
import ariba.util.core.Constants;
import ariba.util.core.PerformanceCheck;
import ariba.util.core.PerformanceState;
import ariba.util.core.ListUtil;
import java.util.List;

public final class AWDebugPane extends AWComponent implements PerformanceCheck.ErrorSink
{
    //----------------------------------------------------------------------
    //  Constants
    //----------------------------------------------------------------------

    protected final static String  AWDebugPanelStateKey = "AWDebugPanelState";
    public final static String  WarningStyle =
        "font-weight:bold!important;color:black!important;background-color:#FFEAAA!important;padding:5px 0px 5px 10px!important";
    public final static String  ErrorStyle =
        "font-weight:bold!important;color:white!important;background-color:red!important;padding:5px 0px 5px 10px!important";

    // TODO:  Need to offer session-level setting...
    protected static boolean PerfWarningsEnabled = true;

    public boolean _showValidationData = false;
    public List /*PerfRec*/ _warningList = null;
    public PerfRec _curWarning;

    public static boolean perfWarningsEnabled ()
    {
        return PerfWarningsEnabled;
    }

    public static void setPerfWarningsEnabled (boolean PerfWarningsEnabled)
    {
        AWDebugPane.PerfWarningsEnabled = PerfWarningsEnabled;
    }

    public boolean isStateless ()
    {
        return false;
    }

    /**
        Always called at end of page.  Check error stats.
     */
    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        _showValidationData = false;
        _warningList = null;
        _curWarning = null;

        // Flush current page size info so it's available in stats bag now
        ((AWBaseResponse)response()).flushSizeStat();

        if (PerfWarningsEnabled) {
            PerformanceState.Stats stats = PerformanceState.getThisThreadHashtable();
            PerformanceCheck checker;
            if (stats != null && (checker = stats.getPerformanceCheck()) != null) {
                checker.checkAndRecord(stats, this);
            }
        }
        super.renderResponse(requestContext, component);
    }

    public AWComponent debugAWOptions ()
    {
        return (AWDebugOptions)pageWithName(AWDebugOptions.Name);
    }

    public boolean hasValidationErrors ()
    {
        return (requestContext().validationContext().hasValidationErrors());
    }

    public boolean hasValidationItems ()
    {
        return requestContext().validationContext().hasErrors();
    }

    public boolean hasGeneralErrors ()
    {
        return requestContext().validationContext().hasGeneralErrors();
    }

    public List getGeneralErrors ()
    {
        return requestContext().validationContext().getGeneralErrors();
    }

    public String errorWarningStyle ()
    {
        if (requestContext().validationContext().hasErrorForComponentPackage(pageComponent())) {
            return ErrorStyle;
        } else {
            return WarningStyle;
        }
    }

    // a global is okay -- this is for test only, and we want it to stick across sessions
    private static boolean _IsOpen = true;

    public boolean isOpen ()
    {
        /*
        Boolean state = (Boolean)session().dict().get(AWDebugPanelStateKey);
        return (state != null) && (state.booleanValue());
        */
        return _IsOpen;
    }

    public void setIsOpen (boolean yn)
    {
        /*
        if (yn) {
            session().dict().put(AWDebugPanelStateKey, Boolean.TRUE);
        } else {
            session().dict().remove(AWDebugPanelStateKey);
        }
        */
        _IsOpen = yn;
    }

    public boolean isDebuggingEnabled ()
    {
        return ariba.ui.aribaweb.core.AWConcreteServerApplication.IsDebuggingEnabled
                && !requestContext().isPrintMode();
    }

    public boolean isComponentPathDebuggingEnabled ()
    {
        AWSession session = requestContext().session(false);
        if (session == null) {
            return false;
        }
        else {
            Boolean flag = (Boolean)session.dict().get(AWConstants.ComponentPathDebugFlagKey);
            return (flag != null) && flag.booleanValue();
        }
    }

    public void togglePathDebugging ()
    {
        boolean shouldEnable = !isComponentPathDebuggingEnabled();
        AWSession session = requestContext().session(false);
        if (session != null) {
            session.dict().put(AWConstants.ComponentPathDebugFlagKey,
                        (shouldEnable ? Boolean.TRUE: Boolean.FALSE ));
        }
        if (shouldEnable) {
            AribaPageContent.setMessage("Path Debugging Enabled!  You may also Alt-click on elements to see the path to a particular part of the page", session());
        }
    }

    public AWComponent showComponentPath ()
    {
        AWComponentInspector page = (AWComponentInspector)pageWithName(AWComponentInspector.class.getName());
        requestContext().stopComponentPathRecording();
        page.init(requestContext().debugTrace());
        requestContext().setFrameName("AWComponentPath");
        return page;
    }

    public AWComponent updateComponentInspector ()
    {
        AWComponentInspector page = (AWComponentInspector)pageWithName(AWComponentInspector.class.getName());
        requestContext().stopComponentPathRecording();
        page.init(requestContext().lastDebugTrace());
        requestContext().setFrameName("AWComponentPath");
        return page;
    }

    public boolean isSessionKeepAliveDebuggingEnabled ()
    {
        AWSession session = requestContext().session(false);

        if (ariba.ui.widgets.Widgets.AWSessionManager != null &&
            session != null) {
            Boolean booleanValue =
                (Boolean)session.dict().get("IsSessionKeepAliveDebuggingEnabled");
            return (booleanValue != null && booleanValue == Boolean.TRUE);
        }
        return false;
    }


    public AWComponent openComponentApiPage ()
    {
        return pageWithName(AWApiPage.Name);
    }

    public AWComponent openValidationErrorPage ()
    {
        AWValidationErrorPage errorPage = (AWValidationErrorPage)pageWithName(AWValidationErrorPage.Name);
        AWValidationContext validationContext = page().validationContext();
        AWComponent pageComponent = pageComponent();
        String pageComponentName = pageComponent.name();
        AWComponentDefinition pageComponentDefinition = pageComponent.componentDefinition();
        String packageName = AWComponentApiManager.packageNameForComponent(pageComponentDefinition);
        errorPage.setup(validationContext, pageComponentName, packageName);
        return errorPage;
    }

    public AWComponent reRender ()
    {
        requestContext().put(AWComponentActionRequestHandler.DebugRerenderCountKey,
                                    Constants.getInteger(rerenderCount()));
        requestContext().put(AWComponentActionRequestHandler.DebugRerenderAsRefreshKey,
                                Constants.getBoolean((AWDebugOptions.RenderAsRefresh)));
        return null;
    }

    public int rerenderCount ()
    {
        return AWDebugOptions.RepeatCount;
    }

    public String rerenderMode ()
    {
        return AWDebugOptions.RenderAsRefresh ? "Refresh" : "Full";
    }

    public void disablePerfWarnings ()
    {
        setPerfWarningsEnabled(false);
    }

    public static class PerfRec
    {
        public PerformanceChecker check;
        public int severity;
        public String valueString;
        public String message;
    }

    public void recordError (PerformanceState.Stats stats, PerformanceChecker check,
                             int severity, String message)
    {
        if (_warningList == null) {
            _warningList = ListUtil.list();
        }
        PerfRec rec = new PerfRec();
        _warningList.add(rec);
        rec.check = check;
        rec.valueString = check.getValueString(stats);
        rec.severity = severity;
        rec.message = message;
    }

    public boolean hasWarnings ()
    {
        return _warningList != null || showValidationDisplayWarning();
    }

    public String warningClass ()
    {
        return (_curWarning.severity == PerformanceCheck.SeverityError) ? "debugError" : "debugWarning";
    }

    public boolean showValidationDisplayWarning ()
    {
        return page().debug_prevPageHasValidationDisplayError();
    }

    /*
        Hook for AWDebugger:
        AWDebugger attaches, interrupts the sleeping AWDebuggerHook thread, and
        then uses it to make calls to the resource manager to resolve tag references.
     */
    static {
        if (AWConcreteApplication.IsDebuggingEnabled) {
            new Thread(new AWDebuggerThread(), "AWDebuggerHook").start();
        }
    }

    public static class AWDebuggerThread implements Runnable
    {

        private static final long _YearMillis = 365L * 24 * 60 * 60 * 1000;
        long lastCall;

        public void run ()
        {
            // this thread just sleeps. when the debugger starts, it
            // puts a breakpoint on breakpointMethod and interrupts this thread.
            while (true) {
                try {
                    Thread.sleep(_YearMillis);
                }
                catch (InterruptedException ie) {
                }
                breakpointMethod();
            }
        }

        // Dummy method for the debugger to put a breakpoint on
        public void breakpointMethod ()
        {
            lastCall = System.currentTimeMillis();
        }

        public String classForTagName (String pack, String tname)
        {
            if (tname.indexOf(":") > -1) {
                AWNamespaceManager.Resolver r = AWNamespaceManager.instance().resolverForPackage(pack);
                if (r != null) {
                    String rname = r.lookup(tname);
                    if (rname != null) {
                        tname = rname;
                    }
                }
            }
            return tname;
        }
    }
}