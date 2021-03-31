package com.swarmus.hivear.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.swarmus.hivear.R;
import com.swarmus.hivear.adapters.CommandViewPagerAdapter;

public class CommandTabs extends Fragment {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_command_tabs, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        CommandViewPagerAdapter commandViewPagerAdapter = new CommandViewPagerAdapter(getChildFragmentManager());
        commandViewPagerAdapter.addFragment(new SwarmSummaryViewFragment(), SwarmSummaryViewFragment.TAB_TITLE);
        commandViewPagerAdapter.addFragment(new SwarmCommands(), SwarmCommands.TAB_TITLE);

        viewPager.setAdapter(commandViewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

        return view;
    }
}