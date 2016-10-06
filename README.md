# HpSmu

Adding HPE Storage Management Utility probes to jrds. This version can monitor controlers, host ports and disk groups.

The package is build using `mvn package`.

Once build, the file must be added to the property `libspath=`:

    libspath=...;hpsmu/target/hpsmu-0.0.1-SNAPSHOT.jar

To use it, define a host to your HP storage, and add the needed probes to it:

    <connection type="fr.jrds.hpsan.HpSanSession" name="restapi">
        <attr name="login">monitor</attr>
        <attr name="password">!monitor</attr>
    </connection>
    <probe type="HpSmuRestPort" connection="restapi">
        <attr name="port">A1</attr>
    </probe>
    ...
    <probe type="HpSmuRestCtrl" connection="restapi">
        <attr name="controller">A</attr>
    </probe>
    ...
    <probe type="HpSmuRestDiskGroup" connection="restapi">
        <attr name="diskgroup">mydg</attr>
    </probe>
