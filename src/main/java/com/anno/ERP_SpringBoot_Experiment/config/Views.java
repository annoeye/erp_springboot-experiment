package com.anno.ERP_SpringBoot_Experiment.config;

public class Views {
    public interface Public {}
    public interface Internal extends Public {}
    public interface User extends Public {}
    public interface Admin extends User {}
}
