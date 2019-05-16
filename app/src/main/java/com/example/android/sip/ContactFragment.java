package com.example.android.sip;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

public class ContactFragment extends Fragment {

    private View view;
    private EditText etSearch;

    private RecyclerView recyclerView;
    private ArrayList<Contact> contactList;


    public ContactFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.contact_fragment, container, false);

        etSearch = (EditText) view.findViewById(R.id.etSearch);

        recyclerView = (RecyclerView) view.findViewById(R.id.rv_contact);
        final RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(getContext(), contactList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(recyclerViewAdapter);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                recyclerViewAdapter.getFilter().filter(s);

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contactList = new ArrayList<>();
        contactList.add(new Contact("demo", "200@ping.com", "200"));
        contactList.add(new Contact("Irfan", "3000@ping.com", "3000"));
        contactList.add(new Contact("Wishma", "3001@ping.com", "3001"));
        contactList.add(new Contact("Rishi", "3002@ping.com", "3002"));
    }
}
